package relevance;

import dal.DbAccess;
import lemmas.LemmasListCreator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.jsoup.safety.Whitelist;

import java.util.*;

import static relevance.Relevancer.getPagesRelRelevance;

public class PageInfo {
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

    public PageInfo(String uri, String title, String snippet, float relevance) {
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    public PageInfo(String uri, String snippet) {
        this.uri = uri;
        this.snippet = snippet;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public float getRelevance() {
        return relevance;
    }

    public void setRelevance(float relevance) {
        this.relevance = relevance;
    }

    public static PageInfo getPageInfoByPageId (int pageId, String searchRequest, float pageIdLemmaRelevance) {


        String uri =  DbAccess.getPageUriAndContent(pageId).uri;

        String content = DbAccess.getPageUriAndContent(pageId).snippet;
        String title =  Jsoup.parse(content).select("title").html();;

//        String snippet =  getSnippetVer2("Заголовок: "+ title + "| Текст: " + Jsoup.clean(content, Safelist.none()),
//                searchRequest);

        String snippet =  getSnippet("Заголовок: "+ title + "| Текст: " + Jsoup.clean(content, Safelist.none()),
                searchRequest);
//        String snippet =  getSnippet(content,                 searchRequest);
        float relevance = pageIdLemmaRelevance;
        return new PageInfo(uri, title,snippet,relevance);
    }

    public String toString() {
        return this.uri + " -> "+ this.title + " -> " + this.snippet + " -> " + this.relevance;
    }


    public static String getSnippet (String inputText, String word) {
        inputText = inputText.toLowerCase(Locale.ROOT);
        int start = inputText.indexOf(word);
        int finish = inputText.lastIndexOf(word);
        if (start != finish) {
            inputText = inputText.substring(start,finish + word.length());
        }
        String out = inputText.replace(word, "<b>" + word + "</b>");
        return out;
    }



    public static String getSnippetVer2(String inputText, String searchRequest) {
        inputText = inputText.toLowerCase(Locale.ROOT);
        List<String> searchRequestlemmaList = LemmasListCreator.getLemmaList(searchRequest);
        List<String> inputTextWordList = LemmasListCreator.prepareWordsList(inputText);
        List<List<String>> inputTextLemmasList = LemmasListCreator.getLemmaList(inputTextWordList);
        List<Integer> matchNumberForWordAndLemma = new ArrayList<>();
        for (int i = 0; i < inputTextWordList.size(); i++) {
            for (String lemmaOfSearchRequest : searchRequestlemmaList) {
                for (String lemmaOfInputTextWord : inputTextLemmasList.get(i)) {
                    if (lemmaOfSearchRequest.equals(lemmaOfInputTextWord)) {
                        matchNumberForWordAndLemma.add(i);
                    }
                }
            }
        }
        return boldFindedWords(matchNumberForWordAndLemma, inputTextWordList, inputText);
    }

    private static String boldFindedWords(List<Integer> numberFindedWordList, List<String> inputTextWords,
                                          String inputTextForBolding) {

        Set<String> uniqueWordForBolding = new LinkedHashSet<>();

        for (Integer num : numberFindedWordList) {
            if (uniqueWordForBolding.add(inputTextWords.get(num))) {
                inputTextForBolding = inputTextForBolding.replaceFirst(inputTextWords.get(num),
                        "<b>" + inputTextWords.get(num) + "</b>");
            }
        }
        if (numberFindedWordList.size() <= 1) {
            return inputTextForBolding;
        }
        return inputTextForBolding.substring(inputTextForBolding.indexOf("<b>"), inputTextForBolding.lastIndexOf(
                "</b>")) + "</b>";
    }





    public static List<PageInfo> getPagesFullRelevanceInfo (String searchRequest, int borderValue) {
        List<PageInfo> pageInfoList = new ArrayList<>();
        HashMap<Integer,Float> pagesRelRelevance = getPagesRelRelevance(searchRequest, borderValue);

        List< Map.Entry <Integer,Float>> tmpList =
                pagesRelRelevance.entrySet().stream().sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).toList();

        for (Map.Entry<Integer,Float> e: tmpList) {
            PageInfo pageInfo = PageInfo.getPageInfoByPageId(e.getKey(), searchRequest, e.getValue());
            pageInfoList.add(pageInfo);
        }
        return pageInfoList;
    }

    public static List<PageInfo> getPagesFullRelevanceInfo (String searchRequest, int borderValue, Integer siteId,
                                                            int offset, int limit) {
        List<PageInfo> pageInfoList = new ArrayList<>();
        HashMap<Integer,Float> pagesRelRelevance = getPagesRelRelevance(searchRequest, borderValue, siteId);

        List< Map.Entry <Integer,Float>> tmpList =
                pagesRelRelevance.entrySet().stream().sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).toList();

        for (Map.Entry<Integer,Float> e: tmpList) {
            PageInfo pageInfo = PageInfo.getPageInfoByPageId(e.getKey(), searchRequest, e.getValue());
            pageInfoList.add(pageInfo);
        }
        System.out.println("Размер возвращаемого списка: " + pageInfoList.size());
//        if (pageInfoList.size() >= offset + limit) {
//            pageInfoList = pageInfoList.subList(offset, offset + limit );
//        } else if (pageInfoList.size() >= offset && pageInfoList.size() < offset + limit) {
//            pageInfoList = pageInfoList.subList(offset, pageInfoList.size());
//        }
//        else {
//            pageInfoList.clear();
//        }

        return cutPagesFullRelevanceInfo(pageInfoList,offset,limit);
    }

    public static List<PageInfo> getPagesFullRelevanceInfo (String searchRequest, int borderValue, Integer siteId) {
        List<PageInfo> pageInfoList = new ArrayList<>();
        HashMap<Integer,Float> pagesRelRelevance = getPagesRelRelevance(searchRequest, borderValue, siteId);

        List< Map.Entry <Integer,Float>> tmpList =
                pagesRelRelevance.entrySet().stream().sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).toList();

        for (Map.Entry<Integer,Float> e: tmpList) {
            PageInfo pageInfo = PageInfo.getPageInfoByPageId(e.getKey(), searchRequest, e.getValue());
            pageInfoList.add(pageInfo);
        }
        System.out.println("Размер возвращаемого списка: " + pageInfoList.size());
//        if (pageInfoList.size() >= offset + limit) {
//            pageInfoList = pageInfoList.subList(offset, offset + limit );
//        } else if (pageInfoList.size() >= offset && pageInfoList.size() < offset + limit) {
//            pageInfoList = pageInfoList.subList(offset, pageInfoList.size());
//        }
//        else {
//            pageInfoList.clear();
//        }

        return pageInfoList;
    }

    public static List<PageInfo> cutPagesFullRelevanceInfo (List<PageInfo> pageInfoList, int offset, int limit ) {
        if (pageInfoList.size() >= offset + limit) {
            pageInfoList = pageInfoList.subList(offset, offset + limit );
        } else if (pageInfoList.size() >= offset && pageInfoList.size() < offset + limit) {
            pageInfoList = pageInfoList.subList(offset, pageInfoList.size());
        }
        else {
            pageInfoList.clear();
        }
        return pageInfoList;
    }

}
