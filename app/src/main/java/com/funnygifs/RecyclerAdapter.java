package com.funnygifs;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.Tracker;

import java.util.List;

import im.ene.lab.toro.ToroAdapter;

import static android.content.Context.MODE_PRIVATE;


public class RecyclerAdapter extends ToroAdapter<SimpleViewHolder> {
    private List<FetchedItem> mFetchedItems;
    private Tracker mTracker;
    private Activity mContext;
    private SimpleViewHolder simpleViewHolder;

    public RecyclerAdapter(List<FetchedItem> galleryItems, Tracker tracker, Activity context) {
        super();
        mFetchedItems = galleryItems;
        mTracker = tracker;
        mContext = context;
    }

    public void downloadAfterPermissionGranted() {
        SharedPreferences prefs = mContext.getSharedPreferences("Download", MODE_PRIVATE);
        String url = prefs.getString("GifUrl","");
        String title = prefs.getString("GifTitle","");
        simpleViewHolder.downloadAfterPermissionGranted(url,title);
    }

    @Override
    protected Object getItem(int position) {
        return mFetchedItems.get(position % mFetchedItems.size());
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view, parent, false);
        simpleViewHolder = new SimpleViewHolder((CardView) v, mTracker, mContext);
        return simpleViewHolder;
    }

    @Override
    public int getItemCount() {
        return mFetchedItems.size();
    }
}