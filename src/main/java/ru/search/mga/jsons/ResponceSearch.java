package ru.search.mga.jsons;

import java.util.ArrayList;
import java.util.List;

public class ResponceSearch {
    private boolean result;
    private int count;

private List<ResponseSearchSiteData> data ;

    public ResponceSearch(boolean result, int count, ArrayList<ResponseSearchSiteData> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<ResponseSearchSiteData> getData() {
        return data;
    }

    public void setData(List<ResponseSearchSiteData> data) {
        this.data = data;
    }
}

