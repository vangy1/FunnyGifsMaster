package com.funnygifs;

public class FetchedItem {
    private String mId;
    private String mUrl;
    private String mDownloadUrl;
    private String mShareUrl;
    private String mTitle;
    private String mPlaceholder;

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getUrl() {
        return mUrl;
    }
    public String getShareUrl() {
        return mShareUrl;
    }
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public String getPlaceholder() {
        return mPlaceholder;
    }

    public void setPlaceholder(String mPlaceholder) {
        this.mPlaceholder = mPlaceholder;
    }

    public void setUrl(String mUrl) {
        this.mUrl = mUrl;
        String mShareUrl = mUrl.replace(".mp4","");
        String mDownloadUrl = mUrl.replace(".mp4",".gif");
        this.mShareUrl = mShareUrl;
        this.mDownloadUrl = mDownloadUrl;
    }
}
