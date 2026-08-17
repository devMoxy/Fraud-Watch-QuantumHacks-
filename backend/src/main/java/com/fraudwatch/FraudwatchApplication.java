package com.fraudwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FraudwatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudwatchApplication.class, args);
    }
}
