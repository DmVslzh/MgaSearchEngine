package jsons;

import dal.DbAccess;

public class TotalSearchEngineInfo {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean isIndexing = false;


    public int getSites() {
        return sites;
    }

    public void setSites(int sites) {
        this.sites = sites;
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

    public boolean isIndexing() {
        return isIndexing;
    }

    public void setIndexing(boolean indexing) {
        isIndexing = indexing;
    }


    public static TotalSearchEngineInfo getTestData() {
        TotalSearchEngineInfo out = new TotalSearchEngineInfo();
        out.setSites(10);
        out.setPages(436423);
        out.setLemmas(5127891);
        out.setIndexing(true);
        return out;
    }

    public static TotalSearchEngineInfo getActualData() {
        TotalSearchEngineInfo out = new TotalSearchEngineInfo();
        out.setSites(DbAccess.getTotalInfo("site"));
        out.setPages(DbAccess.getTotalInfo("page"));
        out.setLemmas(DbAccess.getTotalInfo("lemma"));
        return out;
    }

    public boolean isEmpty() {
        return (getSites() == -1 || getPages() == -1 || getLemmas() == -1) ? true : false;
    }
}
