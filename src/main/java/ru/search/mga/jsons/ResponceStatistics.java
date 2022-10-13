package ru.search.mga.jsons;

public class ResponceStatistics {
    private boolean result;
    private Statistics statistics;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public static ResponceStatistics getResponceStatisticsActualData() {
        ResponceStatistics out = new ResponceStatistics();
        out.result = true;
        out.statistics = Statistics.getActualStatisticsData();
        return out;
    }
}
