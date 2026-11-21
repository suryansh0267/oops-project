package com.itsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // runs SlaMonitor in the background
public class ItsmApplication {

    public static void main(String[] args) {
        // Timestamps are LocalDateTime (no zone) and the browser reads them as local time,
        // so the server must use the users' zone even inside a UTC Docker container.
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata")));
        SpringApplication.run(ItsmApplication.class, args);
    }
}
