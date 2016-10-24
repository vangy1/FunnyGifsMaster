
package com.funnygifs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.funnygifs.Other.ToroVideoView;
import com.funnygifs.Other.ToroVideoViewHolder;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;

import pub.devrel.easypermissions.EasyPermissions;

import static android.content.Context.MODE_PRIVATE;


public class SimpleViewHolder extends ToroVideoViewHolder {
    private Activity context;
    private Tracker tracker;
    private FetchedItem fetchedItem;

    private TextView caption;
    private ImageView thumbnail;
    private ImageView thumbnailMonkey;
    private ProgressBar progress;

    private ImageButton share;
    private ImageButton download;

    private NotificationManager notifyManager;
    private NotificationCompat.Builder builder;

    private int enabledColor;
    private int disabledColor;
    
    private boolean alreadyPlayed;

    public SimpleViewHolder(CardView itemView, Tracker tracker, Activity context) {
        super(itemView);
        this.context = context;
        this.tracker = tracker;

        caption = (TextView) itemView.findViewById(R.id.title);
        thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
        thumbnailMonkey = (ImageView) itemView.findViewById(R.id.thumbnail_monkey);
        progress = (ProgressBar) itemView.findViewById(R.id.progress);

        share = (ImageButton) itemView.findViewById(R.id.share);
        download = (ImageButton) itemView.findViewById(R.id.download);

        enabledColor = ContextCompat.getColor(context, R.color.colorEnabled);
        disabledColor = ContextCompat.getColor(context, R.color.colorDisabled);
    }

    @Override
    protected ToroVideoView findVideoView(View itemView) {
        return (ToroVideoView) itemView.findViewById(R.id.video_view);
    }

    @Override
    public void bind(Object item) {
        fetchedItem = (FetchedItem) item;
        mVideoView.setVideoURI(Uri.parse(fetchedItem.getUrl()));
        thumbnailMonkey.setVisibility(View.VISIBLE);
        progress.getIndeterminateDrawable().setColorFilter(disabledColor, android.graphics.PorterDuff.Mode.MULTIPLY);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareGif();
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Video share")
                        .setAction(fetchedItem.getId())
                        .build());
                thumbnailMonkey.setVisibility(View.INVISIBLE);

            }
        });
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadGif();
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Video download")
                        .setAction(fetchedItem.getId())
                        .build());
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
                .load(fetchedItem.getPlaceholder())
                .fit()
                .centerInside()
                .into(thumbnail);
        caption.setText(fetchedItem.getTitle());
        share.setColorFilter(disabledColor);
        download.setColorFilter(disabledColor);
        caption.setTextColor(disabledColor);
    }

    @Override
    public boolean wantsToPlay() {
        return visibleAreaOffset() >= 0.75;
    }

    @Override
    public void onPlaybackStarted() {
        if (!alreadyPlayed) {
            thumbnailMonkey.setVisibility(View.INVISIBLE);
            progress.setVisibility(View.VISIBLE);
            alreadyPlayed = true;
        }
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Video play")
                .setAction(fetchedItem.getId())
                .build());
    }

    @Override
    public void onPlaybackProgress(int position, int duration) {
        super.onPlaybackProgress(position, duration);
        if (position > 0) {
            thumbnail.animate().alpha(0.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    SimpleViewHolder.super.onPlaybackStarted();
                }
            }).start();
            progress.setVisibility(View.INVISIBLE);
            thumbnailMonkey.setVisibility(View.INVISIBLE);
            caption.setTextColor(enabledColor);
            share.setColorFilter(enabledColor);
            download.setColorFilter(enabledColor);
        }
    }

    @Override
    public void onPlaybackPaused() {
        if (alreadyPlayed) {
            thumbnail.animate().alpha(1.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    SimpleViewHolder.super.onPlaybackPaused();
                }
            }).start();
        }
        caption.setTextColor(disabledColor);
        share.setColorFilter(disabledColor);
        download.setColorFilter(disabledColor);
        alreadyPlayed = false;
        progress.setVisibility(View.INVISIBLE);
        thumbnailMonkey.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onPlaybackError(MediaPlayer mp, int what, int extra) {
        thumbnail.animate().alpha(1.f).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SimpleViewHolder.super.onPlaybackStopped();
            }
        }).start();
        thumbnail.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.INVISIBLE);
        thumbnailMonkey.setVisibility(View.VISIBLE);
        share.setColorFilter(disabledColor);
        download.setColorFilter(disabledColor);
        caption.setTextColor(disabledColor);
        caption.append(" (removed)");
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Removed gif ids")
                .setAction(fetchedItem.getId())
                .build());
        return super.onPlaybackError(mp, what, extra);
    }

    @Override
    public String getVideoId() {
        return "Id: " + getAdapterPosition();
    }

    private void shareGif() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = fetchedItem.getShareUrl();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Funny Gifs");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        Intent share = Intent.createChooser(sharingIntent, "Share this gif via");
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(share);
    }

    private void downloadGif() {
        String[] perms = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(context, perms)) {
            downloadHavePermission();
        } else {
            downloadNoPermission(perms);
        }
    }

    private void downloadHavePermission() {
        fireNotification();
        String url = fetchedItem.getDownloadUrl();
        String title = fetchedItem.getTitle();
        DownloadGif download = new DownloadGif(context, notifyManager, url, title);
        download.execute();
    }

    private void fireNotification() {
        notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(context);
        builder.setContentTitle("Download")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.placeholder);
    }

    private void downloadNoPermission(String[] perms) {
        SharedPreferences.Editor editor = context.getSharedPreferences("Download", MODE_PRIVATE).edit();
        editor.putString("GifUrl", fetchedItem.getUrl());
        editor.putString("GifTitle", fetchedItem.getTitle());
        editor.apply();
        EasyPermissions.requestPermissions(context, "This app needs access to your storage so you can save the gifs.", 10, perms);
    }

    public void downloadAfterPermissionGranted(String url, String title) {
        String[] perms = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(context, perms)) {
            fireNotification();
            DownloadGif download = new DownloadGif(context, notifyManager, url, title);
            download.execute();
        }
    }
}