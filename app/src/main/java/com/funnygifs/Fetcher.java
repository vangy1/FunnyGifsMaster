package com.funnygifs;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Fetcher {

    private String link;

    public Fetcher(String link) {
        this.link = link;
    }

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<FetchedItem> fetchItems() {

        List<FetchedItem> items = new ArrayList<>();

        try {
            String jsonString = getUrlString(link);
            Log.i("Fetch", "Received JSON: " + jsonString);
            JSONArray jsonBody = new JSONArray(jsonString);
            parseItems(items, jsonBody);
        } catch (IOException ioe) {
            Log.e("Fetch", "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e("Fetch", "Failed to parse JSON", je);
        }

        return items;
    }

    private void parseItems(List<FetchedItem> items, JSONArray jsonBody)
            throws IOException, JSONException {

        for (int i = 0; i < jsonBody.length(); i++) {
            JSONObject photoJsonObject = jsonBody.getJSONObject(i);

            FetchedItem item = new FetchedItem();
            item.setId(photoJsonObject.getString("id"));
            item.setTitle(photoJsonObject.getString("title"));
            item.setUrl(photoJsonObject.getString("url"));
            item.setPlaceholder(photoJsonObject.getString("placeholder"));
            items.add(item);
        }
    }

}
