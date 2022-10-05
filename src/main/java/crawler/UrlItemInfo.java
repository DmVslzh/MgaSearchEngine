package crawler;

import java.util.HashMap;

public class UrlItemInfo {
    private String url;
    private int status;
    private String content;
    private HashMap<String,Float> lemmasListWithWeight;

    public UrlItemInfo(String url, int status, String content) {
        this.url = url;
        this.status = status;
        this.content = content;
        this.lemmasListWithWeight = null;
    }


    public UrlItemInfo(String url, int status, String content, HashMap<String, Float> lemmasListWithWeight) {
        this.url = url;
        this.status = status;
        this.content = content;
        this.lemmasListWithWeight = lemmasListWithWeight;
    }

    public HashMap<String, Float> getLemmasListWithWeight() {
        return lemmasListWithWeight;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
