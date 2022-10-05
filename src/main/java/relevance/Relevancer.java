package relevance;

import dal.DbAccess;
import lemmas.LemmasListCreator;

import java.util.*;

public class Relevancer {


    public static List<PageInfo> getDownSortedPageInfoList(String searchRequest, int borderValue) {
        List<PageInfo> out = new ArrayList<>();

        List<Map.Entry<Integer, Float>> tmp = getPagesRelRelevance(searchRequest, borderValue).entrySet().stream().sorted(Map.Entry.<Integer,
                Float>comparingByValue().reversed()).toList();

        return out;
    }

    public static HashMap<Integer, Float> getPagesRelRelevance(String searchRequest, int borderValue) {
        HashMap<Integer, Float> pagesAbsRanks = getPagesAbsRelevance(searchRequest, borderValue);
        if (pagesAbsRanks.size() == 0) {
            return new HashMap<>();
        }
        List<Map.Entry<Integer, Float>> pagesMapEntrySortedList =
                pagesAbsRanks.entrySet().stream().sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).toList();
        // System.out.println(pagesMapEntrySortedList);
        float maxAbsRelevance = pagesMapEntrySortedList.get(0).getValue();
        HashMap<Integer, Float> out = new HashMap<>();
        for (Map.Entry<Integer, Float> entry : pagesAbsRanks.entrySet()) {
            out.put(entry.getKey(), entry.getValue() / maxAbsRelevance);
        }
        return out;
    }

    public static HashMap<Integer, Float> getPagesRelRelevance(String searchRequest, int borderValue, Integer siteId) {
        HashMap<Integer, Float> pagesAbsRanks = getPagesAbsRelevance(searchRequest, borderValue, siteId);
        if (pagesAbsRanks.size() == 0) {
            return new HashMap<>();
        }
        List<Map.Entry<Integer, Float>> pagesMapEntrySortedList =
                pagesAbsRanks.entrySet().stream().sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).toList();
        // System.out.println(pagesMapEntrySortedList);
        float maxAbsRelevance = pagesMapEntrySortedList.get(0).getValue();
        HashMap<Integer, Float> out = new HashMap<>();
        for (Map.Entry<Integer, Float> entry : pagesAbsRanks.entrySet()) {
            out.put(entry.getKey(), entry.getValue() / maxAbsRelevance);
        }
        return out;
    }

    public static HashMap<Integer, Float> getPagesAbsRelevance(String searchRequest, int borderValue) {
        List<String> lemmasSortedList = getlemmasSortedListForSearchRequest(searchRequest, borderValue);
        List<LemmaPagesRank> lemmasPagesRanksList = new ArrayList<>();
        for (String lemma : lemmasSortedList) {
            HashMap<Integer, Float> pageIdWithRank = getPagesIdWithRanks(lemma);
            lemmasPagesRanksList.add(new LemmaPagesRank(lemma, pageIdWithRank));
        }
        HashMap<Integer, Float> pagesAbsRanks = new HashMap<>();
        for (LemmaPagesRank lpr : lemmasPagesRanksList) {
            for (Map.Entry<Integer, Float> pagesRanks : lpr.getPagesRanks().entrySet()) {
                if (!pagesAbsRanks.containsKey(pagesRanks.getKey())) {
                    pagesAbsRanks.put(pagesRanks.getKey(), pagesRanks.getValue());
                } else {
                    float tmp = pagesAbsRanks.get(pagesRanks.getKey());
                    pagesAbsRanks.replace(pagesRanks.getKey(), (tmp + pagesRanks.getValue()));
                }
            }
        }
        return pagesAbsRanks;
    }

    public static HashMap<Integer, Float> getPagesAbsRelevance(String searchRequest, int borderValue, Integer siteId) {
        List<String> lemmasSortedList = getlemmasSortedListForSearchRequest(searchRequest, borderValue, siteId);
        List<LemmaPagesRank> lemmasPagesRanksList = new ArrayList<>();
        for (String lemma : lemmasSortedList) {
            HashMap<Integer, Float> pageIdWithRank = getPagesIdWithRanks(lemma, siteId);
            lemmasPagesRanksList.add(new LemmaPagesRank(lemma, pageIdWithRank));
        }
      //  lemmasPagesRanksList.subList(offset, offset + limit);
        HashMap<Integer, Float> pagesAbsRanks = new HashMap<>();
        for (LemmaPagesRank lpr : lemmasPagesRanksList) {
            for (Map.Entry<Integer, Float> pagesRanks : lpr.getPagesRanks().entrySet()) {
                if (!pagesAbsRanks.containsKey(pagesRanks.getKey())) {
                    pagesAbsRanks.put(pagesRanks.getKey(), pagesRanks.getValue());
                } else {
                    float tmp = pagesAbsRanks.get(pagesRanks.getKey());
                    pagesAbsRanks.replace(pagesRanks.getKey(), (tmp + pagesRanks.getValue()));
                }
            }
        }
        return pagesAbsRanks;
    }

    public static HashMap<Integer, Float> getPagesIdWithRanks(String lemma) {
        int lemmaId = DbAccess.getLemmaIdFromDB(lemma);
        return DbAccess.getIndexedPageIdListWithRanks(lemmaId);
    }

    public static HashMap<Integer, Float> getPagesIdWithRanks(String lemma, Integer siteId) {
        int lemmaId = DbAccess.getLemmaIdFromDB(lemma, siteId);
        return DbAccess.getIndexedPageIdListWithRanks(lemmaId);
    }

    public static List<String> getlemmasSortedListForSearchRequest(String searchRequest, int borderValue) {
        List<String> searchRequestLemmaList = LemmasListCreator.countNumberLemmasEntries(searchRequest).keySet().stream().toList();
        //  System.out.println(searchRequestLemmaList);
        HashMap<String, Integer> tmp = new HashMap<>();
        for (String s : searchRequestLemmaList) {
            Integer frequency = DbAccess.getLemmaFrequencyFromDB(s);
            if (frequency != -1) {
                tmp.put(s, frequency);
            }
        }


        List<Map.Entry<String, Integer>> lemmasMapEntrySortedList =
                tmp.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue())).toList();
        List<String> lemmasSordetList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : lemmasMapEntrySortedList) {
            if (entry.getValue() < borderValue) {
                lemmasSordetList.add(entry.getKey());
            }
        }
        return lemmasSordetList;
    }

    public static List<String> getlemmasSortedListForSearchRequest(String searchRequest, int borderValue,
                                                                   Integer siteId) {
        List<String> searchRequestLemmaList = LemmasListCreator.countNumberLemmasEntries(searchRequest).keySet().stream().toList();
        //  System.out.println(searchRequestLemmaList);
        HashMap<String, Integer> tmp = new HashMap<>();
        for (String s : searchRequestLemmaList) {
            Integer frequency = DbAccess.getLemmaFrequencyFromDB(s,siteId);
            if (frequency != -1) {
                tmp.put(s, frequency);
            }
        }


        List<Map.Entry<String, Integer>> lemmasMapEntrySortedList =
                tmp.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue())).toList();
        List<String> lemmasSordetList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : lemmasMapEntrySortedList) {
            if (entry.getValue() < borderValue) {
                lemmasSordetList.add(entry.getKey());
            }
        }
        return lemmasSordetList;
    }

}
