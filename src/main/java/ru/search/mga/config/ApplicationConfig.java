package ru.search.mga.config;

import ru.search.mga.crawler.UrlCrawler;
import ru.search.mga.dal.DbAccess;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Configuration;
import ru.search.mga.controllers.DefaultController;
import ru.search.mga.controllers.RestFrontEndController;

import java.util.List;


@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
public class ApplicationConfig {

    private String db;

    private String user;

    private String password;

    private String userAgent;

    private String webInterfaceUrl;


    private List<SiteInfo> sites;

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getWebInterfaceUrl() {
        return webInterfaceUrl;
    }

    public void setWebInterfaceUrl(String webInterfaceUrl) {
        this.webInterfaceUrl = webInterfaceUrl;
    }

    public List<SiteInfo> getSites() {
        return sites;
    }

    public void setSites(List<SiteInfo> sites) {
        this.sites = sites;
    }

    public void init() {
        DbAccess.setDbName(db);
        DbAccess.setDbUser(user);
        DbAccess.setDbPass(password);
        DbAccess.getConnection();
        UrlCrawler.setUserAgent(userAgent);
        DefaultController.setWebInterfaceUrl(webInterfaceUrl);
        RestFrontEndController.setSites(sites);
        System.out.println("====================\nИнициализация...\n====================");
        System.out.println("DB: " + DbAccess.getDbName());
        System.out.println("userAgent: " + UrlCrawler.getUserAgent());
        System.out.println("web interface: " + DefaultController.getWebInterfaceUrl());
        System.out.println("user: " + DbAccess.getDbUser());
        System.out.println("password: " + DbAccess.getDbPass());
        System.out.println("====================\nСайты для обработки:\n====================");
        for (SiteInfo si : sites) {
            System.out.println("url: " + si.getUrl());
            System.out.println("name: " + si.getName());
        }
        for (SiteInfo si : sites) {
            if (!DbAccess.hasSiteInDB(si.getName(), si.getUrl())) {
                DbAccess.initInsertSitesForParsingToDB(si.getName(), si.getUrl());
            }
        }
    }
}
