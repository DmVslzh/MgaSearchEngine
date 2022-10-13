package ru.search.mga.jsons;

import ru.search.mga.dal.DbAccess;

import java.util.ArrayList;
import java.util.List;

public class Statistics {
    private TotalSearchEngineInfo total;
    private List<DetailedSiteInfo> detailed;

    public TotalSearchEngineInfo getTotal() {
        return total;
    }

    public void setTotal(TotalSearchEngineInfo total) {
        this.total = total;
    }

    public List<DetailedSiteInfo> getDetailed() {
        return detailed;
    }

    public void setDetailed(List<DetailedSiteInfo> detailed) {
        this.detailed = detailed;
    }

     public static Statistics getActualStatisticsData() {
        Statistics out = new Statistics();
        out.total = TotalSearchEngineInfo.getActualData();
        out.detailed = new ArrayList<>();
        out.detailed = DbAccess.getDetailedSiteInfoList();
        return out;
    }
}
