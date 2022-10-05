package main;

import org.springframework.beans.factory.annotation.Value;

public class SiteInfo {

    @Value("sites.site.url")
    private String url;

    @Value("sites.site.name")
    private String name;

    private String status;

    private long statusTime;

    private String error;

    private int pages;

    private int lemmas;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void display() {
        System.out.println("SiteInfo:" + this);
        System.out.println("url" + url);
        System.out.println("name" + name);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(long statusTime) {
        this.statusTime = statusTime;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getLemmas() {
        return lemmas;
    }

    public void setLemmas(int lemmas) {
        this.lemmas = lemmas;
    }
}