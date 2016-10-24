package com.funnygifs;

public class FetchedItem {
    private String id;
    private String url;
    private String downloadUrl;
    private String shareUrl;
    private String title;
    private String placeholder;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }
    public String getShareUrl() {
        return shareUrl;
    }
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public void setUrl(String url) {
        this.url = url;
        String shareUrl = url.replace(".mp4", "");
        String downloadUrl = url.replace(".mp4", ".gif");
        this.shareUrl = shareUrl;
        this.downloadUrl = downloadUrl;
    }
}
