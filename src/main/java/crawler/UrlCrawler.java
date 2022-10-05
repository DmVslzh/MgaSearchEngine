package crawler;

import dal.DbAccess;
import lemmas.LemmasListCreator;
import main.RestFrontEndController;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class UrlCrawler extends RecursiveAction {

    private String domainName;
    public static Set<String> urlesOverallSet = ConcurrentHashMap.newKeySet();
    private String currentUrlName;
    private static String userAgent;
    private int siteId;
    private static boolean shotdown = false;

public static void resetUrlesOverallSet() {
    urlesOverallSet = ConcurrentHashMap.newKeySet();
}

    public static boolean isShotdown() {
        return shotdown;
    }

    public static void setShotdown(boolean shotdown) {
        UrlCrawler.shotdown = shotdown;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public static void setUserAgent(String userAgent) {
        System.out.println("userAgent: " + userAgent);
        UrlCrawler.userAgent = userAgent;
    }

    public static String getUserAgent() {
        return userAgent;
    }

    private static HashMap<String, Float> fieldListInfo = DbAccess.getSearchFieldInfo();


    public UrlCrawler(String curUrlName, String domainName, int siteId) {
        this.currentUrlName = curUrlName;
        this.domainName = domainName;
        this.siteId = siteId;
        this.currentUrlName = prepareString(this.currentUrlName);
        this.domainName = prepareString(this.domainName);
        UrlItemInfo urlItemInfo = UrlCrawler.ParsePageWithSiteId(this.currentUrlName, this.siteId);
        if (!DbAccess.insertUrlInfoWithSiteId(urlItemInfo.getUrl().replace(this.domainName, "/"),
                urlItemInfo.getStatus(),
                urlItemInfo.getContent(), this.siteId)) {
            return;
        };
        int pageId = DbAccess.getPageIdWithSiteIdFromDB(this.currentUrlName.replace(this.domainName, "/"), this.siteId);


        DbAccess.bulkInsertPageIdAndLemmaId(urlItemInfo,pageId, siteId);
    }

    @Override
    protected void compute() {
        try {
            Document document = Jsoup.connect(this.currentUrlName)
                    .userAgent(userAgent)
                    .referrer("http://www.MgaSearchBot.ru")
                    .get();

            Elements elements = document.select("a");
            Thread.sleep(500);
            List<UrlCrawler> tasks = new ArrayList<>();
            for (Element element : elements) {
                if (!RestFrontEndController.isIndexing()) {return;}

                String tmpUrl = prepareString(element.absUrl("href"));
                 if (isSitePage(tmpUrl) && !tmpUrl.equals(currentUrlName) && !tmpUrl.equals(domainName) &&
                 urlesOverallSet.add(tmpUrl)) {
                    UrlCrawler newCrawler = new UrlCrawler(tmpUrl, this.domainName, this.siteId);
                    newCrawler.fork();
                    tasks.add(newCrawler);
                    System.out.println("added task...");
                }
            }

            for (UrlCrawler task : tasks) {
                task.join();
            }

        } catch (IOException | InterruptedException e) {
            UrlItemInfo item = getResponceStatusException(this.currentUrlName, e);
            if (!urlesOverallSet.contains(item.getUrl())) {
                urlesOverallSet.add(item.getUrl());
            DbAccess.insertUrlInfoWithSiteId(item.getUrl().replace(this.domainName, "/"), item.getStatus(),
                    item.getContent(), this.siteId);}
        }
    }

    public static SiteIndexingStatuses processURLWithSiteId(String url, int siteId) {
        DbAccess.insertStatusAndDateTimeAboutSite(siteId, SiteIndexingStatuses.INDEXING);
        String currentUrlName = url;
        String domainName = currentUrlName;
        UrlCrawler crawler = new UrlCrawler(currentUrlName, domainName, siteId);
        ForkJoinPool fjp = new ForkJoinPool();
        fjp.execute(crawler);
        crawler.join();
       // fjp.shutdown();
        System.out.println("Site id: " + siteId + " -> finished");
        DbAccess.insertStatusAndDateTimeAboutSite(siteId, SiteIndexingStatuses.INDEXED);
        return SiteIndexingStatuses.INDEXED;
    }

    private boolean isSitePage(String url) {
        if (url.startsWith(domainName) && !(url.contains("#"))) {
            return true;
        }
        return false;
    }

    public static String prepareString(String s) {
        if (!s.endsWith("/")) {
            return s + "/";
        }
        return s;
    }


     public static UrlItemInfo ParsePageWithSiteId(String url, int siteId) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("MgaSearchBot 0.1")
                    .timeout(10000)
                    .execute();

            HashMap<String, Integer> lemmasList = getLemmaListForFieldsName(response,
                    fieldListInfo.keySet().stream().toList());
            DbAccess.updateLemmasWithSiteIdInDB(lemmasList, siteId);

            HashMap<String, Float> lemmasListWithWeight = getLemmaListForFieldsNameWithWeight(response, fieldListInfo);
            DbAccess.insertPageParsingInfoToSiteTable(siteId, "NULL");
            return new UrlItemInfo(url, response.statusCode(), response.body().replace("'", "\\\'"), lemmasListWithWeight);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка парсинга");
            DbAccess.insertPageParsingInfoToSiteTable(siteId, e.getMessage());
            return getResponceStatusException(url, e);
        }
    }

    //получение списка лемм и количества вхождений в ответе (body) для списка тегов (pageFieldNameList)
    public static HashMap<String, Integer> getLemmaListForFieldsName(Connection.Response response,
                                                                     List<String> pageFieldNameList) {
        Document document = Jsoup.parse(response.body());
        String tmp = "";
        for (String s : pageFieldNameList) {
            Elements elements = document.select(s);
            for (Element e : elements) {
                Safelist safelist = new Safelist();
                tmp = tmp.concat(Jsoup.clean(e.html(), safelist));
            }
        }
        HashMap<String, Integer> out = LemmasListCreator.countNumberLemmasEntries(tmp);
        return out;
    }

    //получение списка лемм и количества вхождений в ответе (body) для списка тегов (pageFieldNameList) с весами
    public static HashMap<String, Float> getLemmaListForFieldsNameWithWeight(Connection.Response response,
                                                                             HashMap<String, Float> pageFieldNameInfoList) {
        Document document = Jsoup.parse(response.body());
        HashMap<String, Float> out = new HashMap<>();
        for (String s : pageFieldNameInfoList.keySet()) {
            String tmp = "";
            Elements elements = document.select(s);
            for (Element e : elements) {
                Safelist safelist = new Safelist();
                tmp = tmp.concat(Jsoup.clean(e.html(), safelist));
            }
            out = mergeHashMaps(out, LemmasListCreator.countNumberLemmasEntriesWithWeight(tmp,
                    pageFieldNameInfoList.get(s).floatValue()));
        }
        return out;
    }

    private static HashMap<String, Float> mergeHashMaps(HashMap<String, Float> first, HashMap<String, Float> second) {
        for (Map.Entry<String, Float> entrySecond : second.entrySet()) {
            if (first.containsKey(entrySecond.getKey())) {
                first.replace(entrySecond.getKey(), first.get(entrySecond.getKey()) + entrySecond.getValue());
            } else {
                first.put(entrySecond.getKey(), entrySecond.getValue());
            }
        }
        return first;
    }

    private static UrlItemInfo getResponceStatusException(String requestedUrl, Exception e) {
        UrlItemInfo item;
        if (e.getClass().toString().equals("class org.jsoup.HttpStatusException")) {
            HttpStatusException he = (HttpStatusException) e;
            item = new UrlItemInfo(requestedUrl, he.getStatusCode(), "");
        } else {
            item = new UrlItemInfo(requestedUrl, -1, "");
        }
        return item;
    }
}

