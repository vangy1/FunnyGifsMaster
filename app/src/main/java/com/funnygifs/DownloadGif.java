package com.funnygifs;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by rober on 10/23/2016.
 */

public class DownloadGif extends AsyncTask<Void, Integer, Boolean> {
    private Activity context;

    private String link;
    private String fileName;

    private NotificationManager notifyManager;
    private NotificationCompat.Builder builder;

    private URL url;
    private File dir;
    private File path;
    private HttpURLConnection connection;


    DownloadGif(Activity context, NotificationManager notifyManager, String link, String fileName) {
        this.context = context;
        this.link = link;
        this.fileName = fileName;
        this.notifyManager = notifyManager;
        builder = new NotificationCompat.Builder(context);
        builder.setContentTitle("Download")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.placeholder);
        try {
            url = new URL(link);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Funny Gifs");
        path = new File(dir.getAbsolutePath(), fileName + ".gif");

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        builder.setProgress(100, 0, false);
        notifyManager.notify(1, builder.build());
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        builder.setProgress(100, progress[0], false);
        notifyManager.notify(1, builder.build());
    }

    protected Boolean doInBackground(Void... voids) {
        try {
            dir.mkdirs();

            createConnection();
            startDownloading();
            connection.disconnect();

            fireMediaScan();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void onPostExecute(Boolean successfulDownload) {
        super.onPostExecute(successfulDownload);
        if (successfulDownload) {
            Toast.makeText(context, "Gif downloaded into your device!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "There was a problem downloading the gif", Toast.LENGTH_SHORT).show();
        }
        fireDownloadCompleteNotification();

    }

    private void createConnection() {
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startDownloading() {
        try {
            int fileLength = connection.getContentLength();
            FileOutputStream f = new FileOutputStream(path);
            InputStream in = connection.getInputStream();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fireMediaScan() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(path);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    private void fireDownloadCompleteNotification() {
        builder.setContentTitle("Download complete");
        builder.setContentText(fileName);
        builder.setAutoCancel(true);
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator
                + "Funny Gifs" + File.separator + fileName + ".gif")), "image/gif");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
        builder.setContentIntent(contentIntent);
        builder.setProgress(0, 0, false);
        notifyManager.notify(1, builder.build());
    }
}
