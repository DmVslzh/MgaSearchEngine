package ru.search.mga.controllers;

import ru.search.mga.crawler.SiteIndexingStatuses;
import ru.search.mga.crawler.UrlCrawler;
import ru.search.mga.crawler.UrlItemInfo;
import ru.search.mga.dal.DbAccess;
import ru.search.mga.jsons.ResponceSearch;
import ru.search.mga.jsons.ResponceStatistics;
import ru.search.mga.jsons.ResponseSearchSiteData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import ru.search.mga.config.SiteInfo;
import ru.search.mga.relevance.PageInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RestController
public class RestFrontEndController {

    private static List<SiteInfo> sites;
    private static boolean isIndexing = false;
    volatile public static ArrayList<IndexingThread> indexingThreads = new ArrayList<>();
    volatile private static boolean isIndexingForceStoped = false;


    @GetMapping("${webInterfaceUrl}/statistics")
    @ResponseBody
    public Object getStatistics() {

        ResponceStatistics out = ResponceStatistics.getResponceStatisticsActualData();
        if (out.getStatistics().getTotal().isEmpty()) {
            DbAccess.createDB();
            return new ResponseEntity(new ResponceNotSuccess("Было обнаружено повреждение структуры базы " +
                    "данных. Была предпринята попытка пересоздать БД. Перезагрузите страницу"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return out;
    }


    @GetMapping("${webInterfaceUrl}/startIndexing")
    @ResponseBody
    public Object startIndexing() {
        if (!isIndexing) {
            isIndexing = true;
            isIndexingForceStoped = false;
            DbAccess.createDB();
            DbAccess.getClearDbBeforeNewIndexing(sites);
            DbAccess.getConnection();
            UrlCrawler.resetUrlesOverallSet();


            for (SiteInfo si : sites) {
                IndexingThread th = new IndexingThread(si);
                indexingThreads.add(th);
                System.out.println("indexing threads: " + indexingThreads.size());
                th.start();
            }

            SuccessControlThread controlThread = new SuccessControlThread();
            controlThread.start();
            return new ResponseSuccess();
        }
        return new ResponceNotSuccess("Индексация уже запущена");
    }

    @GetMapping("${webInterfaceUrl}/stopIndexing")
    @ResponseBody
    public Object stopIndexing() {
        if (isIndexing) {
            isIndexingForceStoped = true;
            return new ResponseSuccess();
        }
        return new ResponceNotSuccess("Индексация не запущена");
    }

//   Очень полезный пример для получения параметров с именами
//    public String updateFoos(@RequestParam Map<String,String> allParams) {
//        return "Parameters are " + allParams.entrySet();
//    }

    @PostMapping("${webInterfaceUrl}/indexPage")
    @ResponseBody
    public Object indexNewPage(@RequestParam("url") String url) {
        System.out.println("Инициирована попытка индексирования сраницы:" + url);
        ArrayList<String> sitesUrlList = DbAccess.getSitesUrlList();
        for (String siteUrl : sitesUrlList) {
            if (url.trim().startsWith(siteUrl)) {
                int siteId = DbAccess.getSiteIdByUrl(siteUrl);
                prоcessPageForIndex(url, siteUrl, siteId);
                return new ResponseSuccess();
            }
        }
        return new ResponceNotSuccess("Данная страница находиться за пределами сайтов, указанных в " +
                "конфигурационном файле");
    }


    @GetMapping("${webInterfaceUrl}/search")
    @ResponseBody
    public Object search(@RequestParam("query") String query,
                         @RequestParam("site") @Nullable String site, @RequestParam("offset") @Nullable Integer offset,
                         @RequestParam("limit") @Nullable Integer limit) {

        if (query == null || query == "") {
            return new ResponceNotSuccess("Задан пустой поисковый запрос");
        }

        if (offset == null) offset = 0;
        if (limit == null) limit = 20;

        if (site != null) {
            ResponceSearch responceSearch = new ResponceSearch(true, 0, new ArrayList<>());
            ArrayList<ResponseSearchSiteData> tmpData = this.searchOneSite(site, query, offset, limit);
            responceSearch.getData().addAll(tmpData);
            responceSearch.setCount(tmpData.size());

            if (responceSearch.getData().isEmpty()) {
                return new ResponceNotSuccess("Указанная страница не найдена");
            }
            return responceSearch;
        } else {
            ResponceSearch responceSearch = new ResponceSearch(true, 0, new ArrayList<>());
            for (SiteInfo si : sites) {
                ArrayList<ResponseSearchSiteData> tmpData = this.searchOneSite(si.getUrl(), query, 0, -1);
                responceSearch.getData().addAll(tmpData);
                responceSearch.setCount(responceSearch.getCount() + tmpData.size());
            }

            Collections.sort(responceSearch.getData(), Comparator.comparing(ResponseSearchSiteData::getRelevance));
            Collections.reverse(responceSearch.getData());
            responceSearch.setData(RestFrontEndController.cutResponseSearchSiteData(responceSearch.getData()
                    , offset, limit));

            if (responceSearch.getData().isEmpty()) {
                return new ResponceNotSuccess("Указанная страница не найдена");
            }
            return responceSearch;
        }
    }

    private static List<ResponseSearchSiteData> cutResponseSearchSiteData(List<ResponseSearchSiteData> dataList,
                                                                          int offset, int limit) {
        if (dataList.size() >= offset + limit) {
            dataList = dataList.subList(offset, offset + limit);
        } else if (dataList.size() >= offset && dataList.size() < offset + limit) {
            dataList = dataList.subList(offset, dataList.size());
        } else {
            dataList.clear();
        }
        return dataList;
    }

    private ArrayList<ResponseSearchSiteData> searchOneSite(String site, String query, int offset, int limit) {
        int borderValue = 2000;
        ArrayList<ResponseSearchSiteData> out = new ArrayList<>();
        Integer siteId = DbAccess.getSiteIdByUrl(site);
        System.out.println("Поиск по страницам сайта: " + site + " | id сайта: " + siteId);
        List<PageInfo> pageInfoList = PageInfo.getPagesFullRelevanceInfo(query, borderValue, siteId);

        // тут я все-таки сначала применил обрезку с помощью PageInfo.cutPagesFullRelevanceInfo, в потом уже
        // завернул результат в ответ. Иначе фронт работает некорректно.
        if (limit != -1) {
            pageInfoList = PageInfo.cutPagesFullRelevanceInfo(pageInfoList, offset, limit);
        }

        site = removeLastSlash(site);
        for (PageInfo pi : pageInfoList) {
            ResponseSearchSiteData data = new ResponseSearchSiteData(site, DbAccess.getSiteNameById(siteId),
                    pi.getUri(), pi.getTitle(), pi.getSnippet(), pi.getRelevance());
            out.add(data);
        }
        return out;
    }

    private String removeLastSlash(String str) {
        if (str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }


    public static boolean isIndexingForceStoped() {
        return isIndexingForceStoped;
    }

    private void prоcessPageForIndex(String fullPageUrl, String siteUrl, int siteId) {
        String preparedUrl = UrlCrawler.prepareString(fullPageUrl);
        String shortPageUrl = preparedUrl.substring(siteUrl.length() - 1);

        UrlItemInfo newPageInfo = UrlCrawler.ParsePageWithSiteId(preparedUrl, siteId);

        if (!DbAccess.isUrlPresentInDB(shortPageUrl, siteId)) {
            DbAccess.insertUrlInfoWithSiteId(shortPageUrl, newPageInfo.getStatus(), newPageInfo.getContent(), siteId);
            int pageId = DbAccess.getPageIdWithSiteIdFromDB(shortPageUrl, siteId);
            DbAccess.bulkInsertPageIdAndLemmaId(newPageInfo, pageId, siteId);
        } else {
            int pageId = DbAccess.getPageIdWithSiteIdFromDB(shortPageUrl, siteId);
            DbAccess.updateUrlInfoWithSiteId(shortPageUrl, newPageInfo.getStatus(), newPageInfo.getContent(), siteId);
            DbAccess.deleteRecordFromSearchIndex(pageId);
            DbAccess.bulkInsertPageIdAndLemmaId(newPageInfo, pageId, siteId);
        }
    }


    public static List<SiteInfo> getSites() {
        return sites;
    }

    public static void setSites(List<SiteInfo> sites) {
        RestFrontEndController.sites = sites;
    }

    public static boolean isIndexing() {
        return isIndexing;
    }

    public static void setIsIndexing(boolean isIndexing) {
        RestFrontEndController.isIndexing = isIndexing;
    }
}

class ResponceNotSuccess {
    private boolean result = false;
    private String error;

    public ResponceNotSuccess(String error) {
        this.error = error;
    }

    public boolean isResult() {
        return result;
    }

    public String getError() {
        return error;
    }
}

class ResponseSuccess {
    private boolean result = true;

    public boolean isResult() {
        return result;
    }
}

class IndexingThread extends Thread {
    private SiteInfo siteInfo;

    private SiteIndexingStatuses siteFinishStatus = SiteIndexingStatuses.NEW_SITE;

    public SiteInfo getSiteInfo() {
        return siteInfo;
    }

    public IndexingThread(SiteInfo siteInfo) {
        this.siteInfo = siteInfo;
    }

    public void run() {
        System.out.println("Запуск парсинга сайта -  имя: " + siteInfo.getName() + "| url:" + siteInfo.getUrl());
        int siteId = DbAccess.getSiteId(siteInfo.getName());
        DbAccess.insertStatusAndDateTimeAboutSite(siteId, SiteIndexingStatuses.INDEXING);
        siteFinishStatus = UrlCrawler.processURLWithSiteId(siteInfo.getUrl(), DbAccess.getSiteId(siteInfo.getName()));
        while (!isInterrupted()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("Штатный выход");
            }
        }

        DbAccess.insertPageParsingInfoToSiteTable(siteId, "Принудительная остановка" +
                " индексирования");
        DbAccess.insertStatusAndDateTimeAboutSite(siteId, SiteIndexingStatuses.INDEXED);
        return;
    }

    public SiteIndexingStatuses getSiteFinishStatus() {
        return siteFinishStatus;
    }

    public void setSiteFinishStatus(SiteIndexingStatuses siteFinishStatus) {
        this.siteFinishStatus = siteFinishStatus;
    }
}

class SuccessControlThread extends Thread {

    public void run() {

        while (RestFrontEndController.isIndexing()) {
            boolean allThreadsFinishedSuccessfull = true;
            for (IndexingThread th : RestFrontEndController.indexingThreads) {
                allThreadsFinishedSuccessfull =
                        allThreadsFinishedSuccessfull & (th.getSiteFinishStatus().equals(SiteIndexingStatuses.FAILED) | (th.getSiteFinishStatus().equals(SiteIndexingStatuses.INDEXED)));
                int siteId = DbAccess.getSiteId(th.getSiteInfo().getName());
            }
            if (allThreadsFinishedSuccessfull || RestFrontEndController.isIndexingForceStoped()) {
                System.out.println("Остановка индексации...");
                RestFrontEndController.setIsIndexing(false);

                for (IndexingThread th : RestFrontEndController.indexingThreads) {
                    th.interrupt();
                }
                RestFrontEndController.indexingThreads = new ArrayList<>();
                System.out.println("controlThread -> finish indexing");
            }
        }
        this.interrupt();
        return;
    }
}





