package main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import dal.DbAccess;




@SpringBootApplication
public class Main implements CommandLineRunner {

    @Autowired
    private ApplicationConfig config;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Main.class);
        app.run();
        DbAccess.getConnection();
     }

    public void run(String... args) throws Exception {
        config.init();
    }
}


