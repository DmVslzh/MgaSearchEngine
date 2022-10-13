package ru.search.mga;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.search.mga.dal.DbAccess;
import ru.search.mga.config.ApplicationConfig;


@SpringBootApplication
public class SearchEngineApplication implements CommandLineRunner {

    @Autowired
    private ApplicationConfig config;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SearchEngineApplication.class);
        app.run();
        DbAccess.getConnection();
     }

    public void run(String... args) throws Exception {
        config.init();
    }
}


