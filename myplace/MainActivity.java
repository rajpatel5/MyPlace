package com.example.myplace;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements
        ObjectAdapter.ModelAdapterOnClickHandler {

    // Used for logging
    private static final String TAG = MainActivity.class.getSimpleName();

    // IMPORTANT CONSTANTS
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final int WRITE_EXTERNAL_STORAGE = 1024;

    // Path to gallery
    String imagePath = null;

    private ArFragment arFragment;
    private SlidingUpPanelLayout mLayout;
    private ModelRenderable objectRenderable;

    // Used for recyclerViews
    private ObjectAdapter mFirstObjectAdapter;
    private ObjectAdapter mSecondObjectAdapter;
    private ObjectAdapter mThirdObjectAdapter;

    private RecyclerView mFirstRecyclerView;
    private RecyclerView mSecondRecyclerView;
    private RecyclerView mThirdRecyclerView;

    // Stores the id's of the SFB file to the models
    private int[][] modelIds = {{R.raw.armchair, R.raw.beech_chair, R.raw.bookcase_shelf, R.raw.chair, R.raw.cindy_chair},
            {R.raw.dana_chair, R.raw.dresser,R.raw.floor_lamp, R.raw.nightstand, R.raw.malm_bed},
            {R.raw.roma_bed, R.raw.sheppard_chair, R.raw.simple_chair, R.raw.sofa_chair, R.raw.stove},
            {R.raw.patio_chair}
    };

    ArrayList<Drawable> modelImages1 = new ArrayList<>();
    ArrayList<String> modelNames1 = new ArrayList<>();
    ArrayList<Drawable> modelImages2 = new ArrayList<>();
    ArrayList<String> modelNames2 = new ArrayList<>();
    ArrayList<Drawable> modelImages3 = new ArrayList<>();
    ArrayList<String> modelNames3 = new ArrayList<>();

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if device supports AR
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // data to populate the RecyclerViews with
        addModelImages();
        addModelNames();

        mFirstRecyclerView = (RecyclerView) findViewById(R.id.first);
        mSecondRecyclerView = (RecyclerView) findViewById(R.id.second);
        mThirdRecyclerView = (RecyclerView) findViewById(R.id.third);

        LinearLayoutManager firstLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        LinearLayoutManager secondLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        LinearLayoutManager thirdLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        /* setLayoutManager associates the LayoutManager we created above with our RecyclerView */
        mFirstRecyclerView.setLayoutManager(firstLayoutManager);
        mSecondRecyclerView.setLayoutManager(secondLayoutManager);
        mThirdRecyclerView.setLayoutManager(thirdLayoutManager);

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerViews
         */
        mFirstRecyclerView.setHasFixedSize(false);
        mSecondRecyclerView.setHasFixedSize(false);
        mThirdRecyclerView.setHasFixedSize(false);

        /* New ModelAdapters */
        mFirstObjectAdapter = new ObjectAdapter(this, modelImages1, modelNames1,
                0,this);
        mSecondObjectAdapter = new ObjectAdapter(this, modelImages2, modelNames2,
                1,this);
        mThirdObjectAdapter = new ObjectAdapter(this, modelImages3, modelNames3,
                2,this);
        //mFourthObjectAdapter = new ObjectAdapter(this, modelImages4, modelNames4,
                //3,this);

        /* Setting the adapter attaches it to the RecyclerViews in our layout. */
        mFirstRecyclerView.setAdapter(mFirstObjectAdapter);
        mSecondRecyclerView.setAdapter(mSecondObjectAdapter);
        mThirdRecyclerView.setAdapter(mThirdObjectAdapter);

        // Initializes all the buttons and the menu
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        ImageView arrow = (ImageView) findViewById(R.id.menu_arrow);
        FloatingActionButton delete = findViewById(R.id.delete);
        FloatingActionButton takePic = findViewById(R.id.take_pic);

        // Recognizes if user clicks the camera button
        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

                // Getting permission at runtime or taking screenshot
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    takeScreenshot();
                    Toast.makeText(MainActivity.this, "Picture saved in gallery",
                            Toast.LENGTH_LONG).show();
                }else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            WRITE_EXTERNAL_STORAGE);
                }
            }
        });

        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.i(TAG, "onPanelSlide, offset " + slideOffset);
                mLayout.bringToFront();
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {
                Log.i(TAG, "onPanelStateChanged " + newState);
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    arrow.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_arrow_down));
                } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED){
                    arrow.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_arrow_up));
                }
            }
        });

        arFragment.setOnTapArPlaneListener(
            (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                if (objectRenderable == null) {
                    return;
                }

                // Creates the Anchor.
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                // Creates the transformable object and add it to the anchor.
                TransformableNode object = new TransformableNode(arFragment.getTransformationSystem());

                // Prevent User from scaling (MAX must be > MIN)
                object.getScaleController().setMaxScale(1.0001f);
                object.getScaleController().setMinScale(1.0f);

                object.setParent(anchorNode);
                object.setRenderable(objectRenderable);
                object.select();

                deleteObject(delete, anchorNode, object);

                // Allows user to delete a selected object
                object.setOnTapListener((HitTestResult hitTestResult, MotionEvent motionEvent1)->{
                    object.select();
                    deleteObject(delete, anchorNode, hitTestResult.getNode());
                });
            });
    }

    /* Deletes the object if user clicks on the delete button
     */
    private void deleteObject(ImageButton delete, AnchorNode anchorNode, Node object){

        // Deletes the object
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                anchorNode.removeChild(object);
            }
        });
    }
    

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     * Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    // Builds the appropriate model
    public void modelBuilder(int recycleViewID, int modelId){
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
            .setSource(this, modelIds[recycleViewID][modelId])
            .build()
            .thenAccept(renderable -> objectRenderable = renderable)
            .exceptionally(
                    throwable -> {
                        Toast toast =
                                Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return null;
                    });
    }

    public void addModelImages(){
        modelImages1.add(getDrawable(R.drawable.armchair));
        modelImages1.add(getDrawable(R.drawable.beech_chair));
        modelImages1.add(getDrawable(R.drawable.bookcase_shelf));
        modelImages1.add(getDrawable(R.drawable.chair));
        modelImages1.add(getDrawable(R.drawable.cindy_chair));

        modelImages2.add(getDrawable(R.drawable.dana_chair));
        modelImages2.add(getDrawable(R.drawable.dresser));
        modelImages2.add(getDrawable(R.drawable.floor_lamp));
        modelImages2.add(getDrawable(R.drawable.nightstand));
        modelImages2.add(getDrawable(R.drawable.malm_bed));

        modelImages3.add(getDrawable(R.drawable.roma_bed));
        modelImages3.add(getDrawable(R.drawable.sheppard_chair));
        modelImages3.add(getDrawable(R.drawable.simple_chair));
        modelImages3.add(getDrawable(R.drawable.sofa_chair));
        modelImages3.add(getDrawable(R.drawable.stove));

        //modelImages4.add(getDrawable(R.drawable.patio_chair));
    }

    public void addModelNames(){
        modelNames1.add("Armchair");
        modelNames1.add("Beech Chair");
        modelNames1.add("Bookcase Shelf");
        modelNames1.add("Chair");
        modelNames1.add("Cindy Chair");

        modelNames2.add("Dana Chair");
        modelNames2.add("Dresser");
        modelNames2.add("Floor Lamp");
        modelNames2.add("Nightstand");
        modelNames2.add("Malm Bed");

        modelNames3.add("Roma Bed");
        modelNames3.add("Sheppard Chair");
        modelNames3.add("Simple Chair");
        modelNames3.add("Sofa Chair");
        modelNames3.add("Stove");

        //modelNames4.add("Patio Chair");
    }

    @Override
    public void onListItemClick(int recyclerViewId, int clickedItemIndex) {
        // Sets the new model and closes menu
        modelBuilder(recyclerViewId, clickedItemIndex);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    private String generateFilename() {
        String date = new SimpleDateFormat("yyyyMMddHHmmss",
                java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    /* Takes the screenshot through PixelCopy */
    private void takeScreenshot() {
        final String filename = generateFilename();
        ArSceneView view = arFragment.getArSceneView();
        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();

        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(MainActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

                // Store the image to gallery in all Photos section
                imagePath = MediaStore.Images.Media.insertImage(getContentResolver(),
                        bitmap, "title", null);
            } else {
                Log.d("MainActivity", "Failed to copyPixels: " + copyResult);
                Toast toast = Toast.makeText(MainActivity.this,
                        "Failed to take photo: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v("MAIN","Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }
}