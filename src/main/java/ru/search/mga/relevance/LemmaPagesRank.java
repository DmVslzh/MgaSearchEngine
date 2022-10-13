package ru.search.mga.relevance;

import java.util.HashMap;

public class LemmaPagesRank {


    private String lemma;
    private HashMap<Integer,Float> PagesRanks;

    public LemmaPagesRank(String lemma, HashMap<Integer, Float> pagesRank) {
        this.lemma = lemma;
        PagesRanks = pagesRank;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public HashMap<Integer, Float> getPagesRanks() {
        return PagesRanks;
    }

    public void setPagesRanks(HashMap<Integer, Float> pagesRanks) {
        PagesRanks = pagesRanks;
    }
}
