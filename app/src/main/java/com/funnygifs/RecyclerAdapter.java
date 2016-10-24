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
    private List<FetchedItem> fetchedItems;
    private Tracker tracker;
    private Activity context;
    private SimpleViewHolder simpleViewHolder;
    private SharedPreferences prefs;


    public RecyclerAdapter(List<FetchedItem> fetchedItems, Tracker tracker, Activity context) {
        super();
        this.fetchedItems = fetchedItems;
        this.tracker = tracker;
        this.context = context;
        prefs = context.getSharedPreferences("Download", MODE_PRIVATE);
    }

    public void downloadAfterPermissionGranted() {
        String url = prefs.getString("GifUrl","");
        String title = prefs.getString("GifTitle","");
        simpleViewHolder.downloadAfterPermissionGranted(url,title);
    }

    @Override
    protected Object getItem(int position) {
        return fetchedItems.get(position % fetchedItems.size());
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view, parent, false);
        simpleViewHolder = new SimpleViewHolder((CardView) v, tracker, context);
        return simpleViewHolder;
    }

    @Override
    public int getItemCount() {
        return fetchedItems.size();
    }
}