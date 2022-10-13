package ru.search.mga.dal;

import ru.search.mga.crawler.SiteIndexingStatuses;
import ru.search.mga.crawler.UrlItemInfo;
import ru.search.mga.jsons.DetailedSiteInfo;
import ru.search.mga.config.SiteInfo;
import ru.search.mga.relevance.PageInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbAccess {
    public static Connection connection;
    private static String dbName;
    private static String dbUser;
    private static String dbPass;

    public static String getDbName() {
        return dbName;
    }

    public static void setDbName(String dbName) {
        DbAccess.dbName = dbName;
    }

    public static String getDbUser() {
        return dbUser;
    }

    public static void setDbUser(String dbUser) {
        DbAccess.dbUser = dbUser;
    }

    public static String getDbPass() {
        return dbPass;
    }

    public static void setDbPass(String dbPass) {
        DbAccess.dbPass = dbPass;
    }

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(dbName, dbUser, dbPass);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void createDB() {
        try {
            connection.createStatement().execute("DROP TABLE IF EXISTS page");
            connection.createStatement().execute("CREATE TABLE page (" +
                    "id INT primary key NOT NULL AUTO_INCREMENT, " +
                    "path TEXT NOT NULL, " +
                    "code INT NOT NULL, " +
                    "content MEDIUMTEXT NOT NULL, site_id INT NOT NULL)");
            connection.createStatement().execute("CREATE INDEX path ON search_engine.page(path(768))");
            connection.createStatement().execute("DROP TABLE IF EXISTS site");
            connection.createStatement().execute("CREATE TABLE site (" +
                    "id INT primary key NOT NULL AUTO_INCREMENT, " +
                    "status ENUM ('INDEXING', 'INDEXED', 'FAILED') NOT NULL, " +
                    "status_time DATETIME NOT NULL, " +
                    "last_error TEXT, " +
                    "url VARCHAR(255) NOT NULL, " +
                    "name  VARCHAR(255) NOT NULL)");

            connection.createStatement().execute("DROP TABLE IF EXISTS field");
            connection.createStatement().execute(" CREATE TABLE field (" +
                    "id INT primary key NOT NULL AUTO_INCREMENT, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "selector VARCHAR(255) NOT NULL, " +
                    "weight FLOAT NOT NULL)");
            connection.createStatement().execute("INSERT INTO field (name,selector,weight) VALUES " +
                    "('title','title', 1.0), " +
                    "('body', 'body',0.8)");

            connection.createStatement().execute("DROP TABLE IF EXISTS lemma");
            connection.createStatement().execute(" CREATE TABLE lemma (id INT primary key NOT NULL AUTO_INCREMENT, " +
                    "lemma VARCHAR(255) NOT NULL, frequency INT NOT NULL, site_id INT NOT NULL)");

            connection.createStatement().execute("DROP TABLE IF EXISTS search_index");
            connection.createStatement().execute("CREATE TABLE search_index (" +
                    "id INT primary key NOT NULL AUTO_INCREMENT, " +
                    "page_id INT NOT NULL, " +
                    "lemma_id INT NOT NULL, " +
                    "rank_value FLOAT NOT NULL)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean insertUrlInfoWithSiteId(String url, int code, String content, int siteId) {
        String sqlExpression = "insert into page(path,code,content, site_id) VALUES ('" + url + "', " + code + ", '" +
                content + "', " + siteId + ")";
        try {
            connection.createStatement().execute(sqlExpression);
            System.out.println("inserted page info: " + url);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateUrlInfoWithSiteId(String url, int code, String content, int siteId) {
        String sqlExpression =
                "UPDATE page SET code = " + code + ", content = '" + content + "' WHERE path = '" + url + "' AND " +
                        "site_id = " + siteId;
        try {
            connection.createStatement().execute(sqlExpression);
            System.out.println("updated page info: " + url);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isUrlPresentInDB(String url, int siteId) {
        boolean result = false;
        try {
            ResultSet rs =
                    connection.createStatement().executeQuery("SELECT COUNT(*) AS 'urlCounter' FROM page WHERE path =" +
                            " '" + url + "' AND site_id = " + siteId);
            rs.next();
            if (Integer.parseInt(rs.getString("urlCounter")) > 0) {
                result = true;
            } else {
                result = false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int getPageIdWithSiteIdFromDB(String shortUrl, int siteId) {
        int id = -1;
        try {

            ResultSet rs = connection.createStatement().executeQuery("Select * FROM page where path = \"" + shortUrl +
                    "\" AND site_id = " + siteId);
            rs.next();
            System.out.println("url: " + shortUrl);
            id = Integer.parseInt(rs.getString("id"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Page id:" + id);
        return id;
    }

    public static HashMap<Integer, Float> getIndexedPageIdListWithRanks(int lemma_id) {
        HashMap<Integer, Float> out = new HashMap<>();
        try {

            ResultSet rs =
                    connection.createStatement().executeQuery("Select * FROM search_index where lemma_id = \"" + lemma_id +
                            "\"");
            while (rs.next()) {
                int page_id = Integer.parseInt(rs.getString("page_id"));
                Float rank_value = Float.parseFloat(rs.getString("rank_value"));
                out.put(page_id, rank_value);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static int getLemmaIdFromDB(String lemma) {
        int id = -1;
        try {

            ResultSet rs = connection.createStatement().executeQuery("Select * FROM lemma where lemma = \"" + lemma +
                    "\"");
            rs.next();
            id = Integer.parseInt(rs.getString("id"));

        } catch (SQLException e) {
            return id;
        }
        return id;
    }

    public static int getLemmaIdFromDB(String lemma, Integer siteId) {
        int id = -1;
        try {

            ResultSet rs = connection.createStatement().executeQuery("Select * FROM lemma where lemma = \"" + lemma +
                    "\" AND site_id = " + siteId);
            rs.next();
            id = Integer.parseInt(rs.getString("id"));

        } catch (SQLException e) {
            return id;
        }
        return id;
    }


    public static int getLemmaIdWithSiteIdFromDB(String lemma, int siteId) {
        int id = -1;
        try {

            ResultSet rs = connection.createStatement().executeQuery("Select * FROM lemma where lemma = \"" + lemma +
                    "\" AND site_id = \"" + siteId + "\"");
            rs.next();
            id = Integer.parseInt(rs.getString("id"));

        } catch (SQLException e) {
            return id;
        }
        return id;
    }

    public static int getLemmaFrequencyFromDB(String lemma) {
        int frequency = -1;
        try {

            ResultSet rs = connection.createStatement().executeQuery("Select * FROM lemma where lemma = \"" + lemma +
                    "\"");
            rs.next();
            frequency = Integer.parseInt(rs.getString("frequency"));

        } catch (SQLException e) {
            return frequency;
        }
        return frequency;
    }

    public static int getLemmaFrequencyFromDB(String lemma, Integer siteId) {
        int frequency = -1;
        try {

            ResultSet rs = connection.createStatement().executeQuery("Select * FROM lemma where lemma = \"" + lemma +
                    "\" AND site_id = " + siteId.toString());
            rs.next();
            frequency = Integer.parseInt(rs.getString("frequency"));

        } catch (SQLException e) {
            return frequency;
        }
        return frequency;
    }

    public static StringBuilder prepareSqlForBulkInsertPageIdAndLemmaId(StringBuilder sqlExpr, int pageId, int lemmaId,
                                                                        Float rank) {
        if (sqlExpr.length() == 0) {
            sqlExpr = sqlExpr.append("INSERT INTO search_index (page_id,lemma_id,rank_value) VALUES ('" + pageId +
                    "', " + lemmaId +
                    ", '" +
                    rank + "')");
        } else {
            sqlExpr = sqlExpr.append(", ('" + pageId + "', " + lemmaId + ", '" + rank + "')");
        }
        return sqlExpr;
    }

    public static void bulkInsertPageIdAndLemmaId(UrlItemInfo urlItemInfo, int pageId, int siteId) {
        if (urlItemInfo.getStatus() == 200 && pageId != -1) {
            StringBuilder sql = new StringBuilder();
            for (Map.Entry<String, Float> lemmaWithWeight : urlItemInfo.getLemmasListWithWeight().entrySet()) {
                int lemmaId = DbAccess.getLemmaIdWithSiteIdFromDB(lemmaWithWeight.getKey(), siteId);
                if (lemmaId != -1) {
                    DbAccess.prepareSqlForBulkInsertPageIdAndLemmaId(sql, pageId, lemmaId, lemmaWithWeight.getValue());
                }
            }
            DbAccess.executeSQL(sql.toString());
        }
    }

    public static void deleteRecordFromSearchIndex(int pageId) {
        String sql = "DELETE FROM search_index WHERE page_id =" + pageId;
        executeSQL(sql);
    }


    public static void executeSQL(String sql) {
        try {
            connection.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, Float> getSearchFieldInfo() {
        HashMap<String, Float> out = new HashMap<>();
        try {
            ResultSet rs = connection.createStatement().executeQuery("Select * FROM field");
            while (rs.next()) {
                out.put(rs.getString("selector"), Float.parseFloat(rs.getString("weight")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }


    public static HashMap<String, Integer> getLemmaWithFreqFromDB() {
        HashMap<String, Integer> out = new HashMap<>();
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM lemma");
            while (rs.next()) {
                out.put(rs.getString("lemma"), Integer.parseInt(rs.getString("frequency")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static HashMap<String, Integer> getLemmaWithFreqFromDB(int site_id) {
        HashMap<String, Integer> out = new HashMap<>();
        try {
            ResultSet rs =
                    connection.createStatement().executeQuery("SELECT * FROM lemma WHERE site_id = '" + site_id + "'");
            while (rs.next()) {
                out.put(rs.getString("lemma"), Integer.parseInt(rs.getString("frequency")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static void updateLemmasInDB(HashMap<String, Integer> lemmasWithCountsList) {
        HashMap<String, Integer> lemmasInDB = getLemmaWithFreqFromDB();
        for (Map.Entry<String, Integer> newEntry : lemmasWithCountsList.entrySet()) {
            if (lemmasInDB.containsKey(newEntry.getKey())) {
                try {
                    int newFrequency = lemmasInDB.get(newEntry.getKey()) + 1;
                    connection.createStatement().execute("UPDATE lemma SET frequency = " + newFrequency +
                            " WHERE lemma = \"" + newEntry.getKey() + "\";");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    connection.createStatement().execute("INSERT INTO lemma (lemma, frequency) value (\"" + newEntry.getKey() + "\", 1)");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void updateLemmasWithSiteIdInDB(HashMap<String, Integer> lemmasWithCountsList, int siteId) {
        HashMap<String, Integer> lemmasInDB = getLemmaWithFreqFromDB(siteId);
        for (Map.Entry<String, Integer> newEntry : lemmasWithCountsList.entrySet()) {
            if (lemmasInDB.containsKey(newEntry.getKey())) {
                try {
                    int newFrequency = lemmasInDB.get(newEntry.getKey()) + 1;
                    connection.createStatement().execute("UPDATE lemma SET frequency = " + newFrequency +
                            " WHERE lemma = \"" + newEntry.getKey() + "\" AND site_id = \"" + siteId + "\"");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    connection.createStatement().execute("INSERT INTO lemma (lemma, frequency, site_id) value (\"" + newEntry.getKey() + "\", 1, \"" + siteId + "\")");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static PageInfo getPageUriAndContent(int pageId) {
        String uri = "";
        String content = "";
        try {

            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM page where id = \"" + pageId +
                    "\"");
            rs.next();
            uri = rs.getString("path");
            content = rs.getString("content");

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return new PageInfo(uri, content);
    }

    public static int getTotalInfo(String tableName) {
        int out = -1;
        try {
            ResultSet rs =
                    connection.createStatement().executeQuery("SELECT COUNT(*) AS 'totalTableInfo' FROM " + tableName);
            rs.next();
            out = Integer.parseInt(rs.getString("totalTableInfo"));
        } catch (SQLException e) {
            e.printStackTrace();
            return out;
        }
        return out;
    }

    public static ArrayList<Integer> getSitesIdList() {
        ArrayList<Integer> out = new ArrayList<>();
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT id FROM site");
            while (rs.next()) {
                out.add(Integer.parseInt(rs.getString("id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static String getSiteNameById(int siteId) {
        String siteName = "";
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT name FROM site WHERE id = " + siteId);
            rs.next();
            siteName = rs.getString("name");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return siteName;
    }

    public static ArrayList<String> getSitesUrlList() {
        ArrayList<String> out = new ArrayList<>();
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT url FROM site");
            while (rs.next()) {
                out.add(rs.getString("url"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static int getSiteId(String name) {
        int out = -1;

        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT id FROM site WHERE name = '" + name + "'");
            rs.next();
            out = Integer.parseInt(rs.getString("id"));

        } catch (SQLException e) {
            return out;
        }

        return out;
    }

    public static int getSiteIdByUrl(String url) {
        int out = -1;

        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT id FROM site WHERE url = '" + url + "'");
            rs.next();
            out = Integer.parseInt(rs.getString("id"));

        } catch (SQLException e) {
            return out;
        }

        return out;
    }

    public static int getPagesCountBySiteId(int siteId) {
        int out = 0;
        try {
            ResultSet rs =
                    connection.createStatement().executeQuery("SELECT COUNT(id) AS 'countPages' FROM page WHERE " +
                            "site_id = " + siteId);
            rs.next();
            out = Integer.parseInt(rs.getString("countPages"));
        } catch (SQLException e) {
            e.printStackTrace();
            return out;
        }
        return out;
    }

    public static int getLemmasCountBySiteId(int siteId) {
        int out = 0;
        try {
            ResultSet rs =
                    connection.createStatement().executeQuery("SELECT COUNT(id) AS 'countLemmas' FROM lemma WHERE " +
                            "site_id = " + siteId);
            rs.next();
            out = Integer.parseInt(rs.getString("countLemmas"));
        } catch (SQLException e) {
            e.printStackTrace();
            return out;
        }
        return out;
    }

    public static DetailedSiteInfo getDetailedSiteInfoBySiteId(int siteId) {
        DetailedSiteInfo out = new DetailedSiteInfo();
        try {
            ResultSet rs =
                    connection.createStatement().executeQuery("SELECT * FROM site WHERE id = " + siteId);
            rs.next();
            out.setUrl(rs.getString("url"));
            out.setName(rs.getString("name"));
            out.setStatus(rs.getString("status"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDateTime = LocalDateTime.parse(rs.getString("status_time"), formatter);
            ZonedDateTime zdt = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
            long dateInMillis = zdt.toInstant().toEpochMilli();

            out.setStatusTime(dateInMillis);
            out.setError(rs.getString("last_error"));
            out.setPages(getPagesCountBySiteId(siteId));
            out.setLemmas(getLemmasCountBySiteId(siteId));
        } catch (SQLException e) {
            e.printStackTrace();
            return out;
        }
        return out;
    }

    public static List<DetailedSiteInfo> getDetailedSiteInfoList() {
        ArrayList<DetailedSiteInfo> out = new ArrayList<>();
        ArrayList<Integer> sitesIdList = getSitesIdList();
        for (Integer id : sitesIdList) {
            out.add(getDetailedSiteInfoBySiteId(id));
        }
        return out;
    }

    public static boolean hasSiteInDB(String nameSite, String urlSite) {
        boolean result = false;
        try {
            ResultSet rs =
                    connection.createStatement().executeQuery("SELECT COUNT(id) AS 'countSiteEntries' FROM site " +
                            "WHERE name = '" + nameSite + "' AND  url = '" + urlSite + "'");
            rs.next();
            result = Integer.parseInt(rs.getString("countSiteEntries")) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return result;
        }
        return result;
    }

    public static void initInsertSitesForParsingToDB(String nameSite, String urlSite) {
        try {
            LocalDateTime thisDateTime = LocalDateTime.now();
            connection.createStatement().execute("INSERT INTO site (status, status_time, last_error, url, name) VALUES " +
                    "('FAILED', '" + thisDateTime + " ', 'Индексация этого сайта еще не начиналась', '" + urlSite +
                    "', '" + nameSite +
                    "')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertPageParsingInfoToSiteTable(int siteId, String errorDescription) {
        try {
            LocalDateTime thisDateTime = LocalDateTime.now();
            connection.createStatement().execute("UPDATE site SET status_time = '" + thisDateTime + "', last_error = '" + errorDescription + "' WHERE id = " + siteId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertStatusAndDateTimeAboutSite(int siteId, SiteIndexingStatuses status) {
        try {
            LocalDateTime thisDateTime = LocalDateTime.now();
            connection.createStatement().execute("UPDATE site SET status = '" + status + "', status_time = '" + thisDateTime + "', " +
                    "last_error = 'NULL' WHERE id = " + siteId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void getClearDbBeforeNewIndexing(List<SiteInfo> sitesForIndexing) {

        DbAccess.createDB();
        for (SiteInfo si : sitesForIndexing) {
            DbAccess.initInsertSitesForParsingToDB(si.getName(), si.getUrl());
        }
    }
}