
package com.funnygifs;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.Toast;

import com.funnygifs.Other.ToroVideoView;
import com.funnygifs.Other.ToroVideoViewHolder;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import pub.devrel.easypermissions.EasyPermissions;

import static android.content.Context.MODE_PRIVATE;


public class SimpleViewHolder extends ToroVideoViewHolder {


    private final String TAG = getClass().getSimpleName();

    private TextView mCaption;
    private ImageView mThumbnail;
    private ImageView mThumbnailMonkey;
    private ImageButton mShare;
    private ImageButton mDownload;
    private ProgressBar mProgress;
    private FetchedItem mItem;
    private Tracker mTracker;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private boolean alreadyPlayed;
    private Activity mContext;


    public SimpleViewHolder(CardView itemView, Tracker tracker, Activity context) {
        super(itemView);
        mThumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
        mThumbnailMonkey = (ImageView) itemView.findViewById(R.id.thumbnail_monkey);
        mShare = (ImageButton) itemView.findViewById(R.id.share);
        mDownload = (ImageButton) itemView.findViewById(R.id.download);
        mProgress = (ProgressBar) itemView.findViewById(R.id.progress);
        mCaption = (TextView) itemView.findViewById(R.id.title);
        mContext = context;
        mTracker = tracker;
    }

    @Override
    protected ToroVideoView findVideoView(View itemView) {
        return (ToroVideoView) itemView.findViewById(R.id.video_view);
    }

    @Override
    public void bind(Object item) {
        mItem = (FetchedItem) item;
        mVideoView.setVideoURI(Uri.parse(mItem.getUrl()));
        mThumbnailMonkey.setVisibility(View.VISIBLE);
        mProgress.getIndeterminateDrawable().setColorFilter(Color.parseColor("#CCCCCC"), android.graphics.PorterDuff.Mode.MULTIPLY);
        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = mItem.getShareUrl();
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Funny Gifs");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                Intent new_intent = Intent.createChooser(sharingIntent, "Share this gif via");
                new_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Video share")
                        .setAction(mItem.getId())
                        .build());
                mContext.startActivity(new_intent);
                mThumbnailMonkey.setVisibility(View.INVISIBLE);

            }
        });
        mDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String[] perms = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
                String url = mItem.getDownloadUrl();
                String title = mItem.getTitle();
                if (EasyPermissions.hasPermissions(mContext, perms)) {
                    mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    mBuilder = new NotificationCompat.Builder(mContext);
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Video download")
                            .setAction(mItem.getId())
                            .build());
                    mBuilder.setContentTitle("Download")
                            .setContentText("Download in progress")
                            .setSmallIcon(R.drawable.placeholder);
                    Download download = new Download(url, title);
                    download.execute();
                } else {
                    SharedPreferences.Editor editor = mContext.getSharedPreferences("Download", MODE_PRIVATE).edit();
                    editor.putString("GifUrl", mItem.getUrl());
                    editor.putString("GifTitle", mItem.getTitle());
                    editor.apply();
                    EasyPermissions.requestPermissions(mContext, "This app needs access to your storage so you can save the gifs.", 10, perms);
                }
            }
        });
    }

    @Override
    public boolean isLoopAble() {
        return true;
    }


    @Override
    public void onVideoPrepared(MediaPlayer mp) {
        super.onVideoPrepared(mp);
    }

    @Override
    public void onViewHolderBound() {
        super.onViewHolderBound();
        Picasso.with(itemView.getContext())
                .load(mItem.getPlaceholder())
                .fit()
                .centerInside()
                .into(mThumbnail);
        mCaption.setText(mItem.getTitle());
        mShare.setColorFilter(Color.parseColor("#CCCCCC"));
        mDownload.setColorFilter(Color.parseColor("#CCCCCC"));
        mCaption.setTextColor(Color.parseColor("#CCCCCC"));
    }

    @Override
    public boolean wantsToPlay() {
        return visibleAreaOffset() >= 0.75;
    }

    @Override
    public void onPlaybackStarted() {
        if (!alreadyPlayed) {
            mThumbnailMonkey.setVisibility(View.INVISIBLE);
            mProgress.setVisibility(View.VISIBLE);
            alreadyPlayed = true;
        }
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Video play")
                .setAction(mItem.getId())
                .build());
    }

    @Override
    public void onPlaybackProgress(int position, int duration) {
        super.onPlaybackProgress(position, duration);
        if (position > 0) {
            mThumbnail.animate().alpha(0.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    SimpleViewHolder.super.onPlaybackStarted();
                }
            }).start();
            mProgress.setVisibility(View.INVISIBLE);
            mThumbnailMonkey.setVisibility(View.INVISIBLE);
            mShare.setColorFilter(Color.parseColor("#000000"));
            mDownload.setColorFilter(Color.parseColor("#000000"));
            mCaption.setTextColor(Color.parseColor("#000000"));
        }
    }

    @Override
    public void onPlaybackPaused() {
        if (alreadyPlayed) {
            mThumbnail.animate().alpha(1.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    SimpleViewHolder.super.onPlaybackPaused();
                }
            }).start();
        }
        mShare.setColorFilter(Color.parseColor("#CCCCCC"));
        mDownload.setColorFilter(Color.parseColor("#CCCCCC"));
        mCaption.setTextColor(Color.parseColor("#CCCCCC"));
        alreadyPlayed = false;
        mProgress.setVisibility(View.INVISIBLE);
        mThumbnailMonkey.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onPlaybackError(MediaPlayer mp, int what, int extra) {
        mThumbnail.animate().alpha(1.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SimpleViewHolder.super.onPlaybackStopped();
            }
        }).start();
        mThumbnail.setVisibility(View.INVISIBLE);
        mProgress.setVisibility(View.INVISIBLE);
        mThumbnailMonkey.setVisibility(View.VISIBLE);
        mShare.setColorFilter(Color.parseColor("#CCCCCC"));
        mDownload.setColorFilter(Color.parseColor("#CCCCCC"));
        mCaption.setTextColor(Color.parseColor("#CCCCCC"));
        mCaption.append(" (removed)");
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Removed gif ids")
                .setAction(mItem.getId())
                .build());
        return super.onPlaybackError(mp, what, extra);
    }

    @Override
    public String getVideoId() {
        return "TEST: " + getAdapterPosition();
    }


    private class Download extends AsyncTask<Void, Integer, Boolean> {
        private String link;
        private String name;

        Download(String linkPassed, String filename) {
            link = linkPassed;
            name = filename;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mBuilder.setProgress(100, 0, false);
            mNotifyManager.notify(1, mBuilder.build());
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mBuilder.setProgress(100, progress[0], false);
            mNotifyManager.notify(1, mBuilder.build());
            super.onProgressUpdate(progress);
        }

        protected Boolean doInBackground(Void... voids) {
            try {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Funny Gifs");
                dir.mkdirs();
                File path = new File(dir.getAbsolutePath(), name + ".gif");
                URL u = new URL(link);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.connect();
                int fileLength = c.getContentLength();
                FileOutputStream f = new FileOutputStream(path);
                InputStream in = c.getInputStream();
                byte[] buffer = new byte[1024];
                int total = 0;
                int totalLen = 0;
                int oldTotal = 0;
                int len1 = 0;
                while ((len1 = in.read(buffer)) > 0) {
                    totalLen += len1;
                    total = totalLen * 100 / fileLength;
                    if (fileLength > 0 && (total - oldTotal) > 5) {
                        publishProgress(total);
                        oldTotal = total;
                    }
                    f.write(buffer, 0, len1);
                }
                f.close();
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(path);
                mediaScanIntent.setData(contentUri);
                mContext.sendBroadcast(mediaScanIntent);
                return true;
            } catch (Exception e) {
                Log.d("Error....", e.toString());
                return false;
            }
        }

        protected void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);
            if (bool)
                Toast.makeText(mContext, "Gif downloaded into your device!", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(mContext, "There was a problem downloading the gif", Toast.LENGTH_SHORT).show();
            mBuilder.setContentTitle("Download complete");
            mBuilder.setContentText(name);
            mBuilder.setAutoCancel(true);
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator
                    + "Funny Gifs" + File.separator + name + ".gif")), "image/gif");
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
            mBuilder.setContentIntent(contentIntent);
            mBuilder.setProgress(0, 0, false);
            mNotifyManager.notify(1, mBuilder.build());

        }
    }


    public void downloadAfterPermissionGranted(String url, String title) {
        String[] perms = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(mContext, perms)) {
            mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(mContext);
            mBuilder.setContentTitle("Download")
                    .setContentText("Download in progress")
                    .setSmallIcon(R.drawable.placeholder);
            Download download = new Download(url, title);
            download.execute();
        }
    }
}