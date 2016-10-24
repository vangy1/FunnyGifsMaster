package com.funnygifs;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;

import im.ene.lab.toro.Toro;


public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbarTitle;

    private Tracker mTracker;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private LinearLayoutManager mLayoutManager;
    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;

    private List<FetchedItem> mItems = new ArrayList<>();
    private int previousTotal = 0;
    private int visibleThreshold = 5;
    private boolean loading = true;

    private int firstVisibleItem;
    private int visibleItemCount;
    private int totalItemCount;

    private String idOldest;
    private String idNewest;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toro.attach(this);

        findViewsByIds();
        setToolbar();
        setupAnalytics();
        getSharedPrefrences();
        setupRecyclerView();

        Toro.register(recyclerView);

        if (prefs.contains("idNew") && prefs.contains("idOld")) {
            String idNewest = prefs.getString("idNew", "1000");
            String idOldest = prefs.getString("idOld", "1000");
            new FetchItemsTask("http://funnygifs.media/getposts?before=" + idNewest + "&after=" + idOldest).execute();
        } else {
            new FetchItemsTask("http://funnygifs.media/getposts").execute();
        }

    }

    private void findViewsByIds() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        Typeface serifa = Typeface.createFromAsset(getAssets(), "Serifa.ttf");
        toolbarTitle.setTypeface(serifa);
    }

    private void setupAnalytics() {
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        sendScreenImageName();
    }

    private void getSharedPrefrences() {
        prefs = getSharedPreferences("Positions", MODE_PRIVATE);
        editor = prefs.edit();
    }

    private void setupRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!mItems.isEmpty()) {
                    super.onScrolled(recyclerView, dx, dy);

                    getItemsCountAndFirstVisible();
                    getOldestAndNewestIds();
                    updateViewedGifs();
                    checkIfLoadingIsDone();
                    loadNewGifs();
                }
            }
        });
    }

    private void getItemsCountAndFirstVisible() {
        visibleItemCount = recyclerView.getChildCount();
        totalItemCount = mLayoutManager.getItemCount();
        firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
    }

    private void getOldestAndNewestIds() {
        idOldest = mItems.get(mLayoutManager.findLastVisibleItemPosition()).getId();
        idNewest = mItems.get(0).getId();
    }

    private void updateViewedGifs() {
        if (prefs.contains("idNew") && prefs.contains("idOld")) {
            String idNewestBefore = prefs.getString("idNew", "9999");
            String idOldestBefore = prefs.getString("idOld", "9999");

            if (Integer.valueOf(idNewest) > Integer.valueOf(idNewestBefore))
                editor.putString("idNew", idNewest);
            if (Integer.valueOf(idOldest) < Integer.valueOf(idOldestBefore))
                editor.putString("idOld", idOldest);
            editor.apply();
        } else {
            editor.putString("idNew", idNewest);
            editor.putString("idOld", idOldest);
            editor.apply();
        }
    }

    private void checkIfLoadingIsDone() {
        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false;
                previousTotal = totalItemCount;
            }
        }
    }

    private void loadNewGifs() {
        if (!loading && (totalItemCount - visibleItemCount)
                <= (firstVisibleItem + visibleThreshold)) {
            loading = true;

            idNewest = mItems.get(0).getId();
            idOldest = mItems.get(mItems.size() - 1).getId();

            updateViewedGifs();

            new FetchItemsTask("http://funnygifs.media/getposts?before=" + idNewest + "&after=" + idOldest).execute();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<FetchedItem>> {
        private String link;

        FetchItemsTask(String link) {
            this.link = link;
        }

        @Override
        protected List<FetchedItem> doInBackground(Void... params) {
            return new Fetcher(link).fetchItems();
        }

        @Override
        protected void onPostExecute(List<FetchedItem> items) {
            mItems.addAll(items);
            if (recyclerView.getAdapter() == null) {
                recyclerAdapter = new RecyclerAdapter(mItems, mTracker, MainActivity.this);
                recyclerView.setAdapter(recyclerAdapter);
            } else
                recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.refresh) {
            if (!mItems.isEmpty()) {

                idNewest = mItems.get(0).getId();
                idOldest = mItems.get(mLayoutManager.findLastVisibleItemPosition()).getId();

                updateViewedGifs();

                mItems.clear();

                if (prefs.contains("idNew") && prefs.contains("idOld")) {
                    idNewest = prefs.getString("idNew", "1000");
                    idOldest = prefs.getString("idOld", "1000");
                    new FetchItemsTask("http://funnygifs.media/getposts?before=" + idNewest + "&after=" + idOldest).execute();
                } else {
                    new FetchItemsTask("http://funnygifs.media/getposts").execute();
                }
                recyclerView.scrollToPosition(0);
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        Toro.unregister(recyclerView);
        Toro.detach(this);
        super.onDestroy();
    }

    private void sendScreenImageName() {
        mTracker.setScreenName("Main Activity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == 0)
            recyclerAdapter.downloadAfterPermissionGranted();
        if (grantResults[0] == -1) {
            Toast.makeText(this, "You cannot download gifs without the permission", Toast.LENGTH_SHORT).show();
        }
    }
}
