package com.mini2.newsdisplayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NewsDisplayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsDisplayServiceApplication.class, args);
    }

}
