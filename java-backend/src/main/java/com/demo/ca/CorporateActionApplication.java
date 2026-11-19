package com.demo.ca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CorporateActionApplication {
    public static void main(String[] args) {
        SpringApplication.run(CorporateActionApplication.class, args);
    }
}
