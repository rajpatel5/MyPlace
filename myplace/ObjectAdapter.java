package com.example.myplace;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ObjectAdapter extends RecyclerView.Adapter<ObjectAdapter.ViewHolder> {

    // Used for logging
    private final String TAG = MainActivity.class.getSimpleName();

    private List<Drawable> mModelImage;
    private List<String> mModelName;
    private int mRecyclerViewId;
    private LayoutInflater mInflater;

    /** Interface to handle clicks on items within this Adapter.*/
    private ModelAdapterOnClickHandler mClickHandler;

    /** The interface that receives onClick messages. */
    public interface ModelAdapterOnClickHandler {
        void onListItemClick(int recyclerViewID ,int clickedItemIndex);
    }

    /**
     * Creates a ObjectAdapter.
     *
     * @param context      Used to talk to the UI and app resources
     */
    public ObjectAdapter(@NonNull Context context, List<Drawable> modelImage,
                         List<String> modelName, int recyclerViewId,
                         ModelAdapterOnClickHandler clickHandler) {
        this.mInflater = LayoutInflater.from(context);
        mModelImage = modelImage;
        mModelName = modelName;
        mRecyclerViewId = recyclerViewId;
        mClickHandler = clickHandler;
    }

    // inflates the row layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.menu_list_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the views
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Drawable image = mModelImage.get(position);
        String name = mModelName.get(position);
        holder.nameView.setText(name);
        holder.modelView.setImageDrawable(image);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mModelName.size();
    }

    /**
     * A ViewHolder is a required part of the pattern for RecyclerViews. It mostly behaves as
     * a cache of the child views for a transaction item. It's also a convenient place to set an
     * OnClickListener, since it has access to the adapter and the views.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView modelView;
        final TextView nameView;

        ViewHolder(View view) {
            super(view);
            modelView = (ImageView) view.findViewById(R.id.model_image);
            nameView = (TextView) view.findViewById(R.id.model_name);
            view.setOnClickListener(this);
        }


        /**
         * This gets called by the child views during a click. We fetch the date that has been
         * selected, and then call the onClick handler registered with this adapter, passing that
         * date.
         *
         * @param view the View that was clicked
         */
        @Override
        public void onClick(View view) {
            if (mClickHandler != null){
                mClickHandler.onListItemClick(getRecyclerViewId() ,getAdapterPosition());
            }
        }
    }

    // convenience method for getting data at click position
    public String getItem(int id) {
        return mModelName.get(id);
    }

    // convenience method for getting data at click position
    public int getRecyclerViewId() {
        return mRecyclerViewId;
    }

    // allows clicks events to be caught
    public void setClickListener(ModelAdapterOnClickHandler itemClickListener) {
        mClickHandler = itemClickListener;
    }
}
