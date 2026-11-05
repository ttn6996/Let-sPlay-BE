package com.letsplay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LetsPlayApplication {
    public static void main(String[] args) {
        SpringApplication.run(LetsPlayApplication.class, args);
    }
}
