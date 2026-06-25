package com.safelearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SafeLearnApplication {
    public static void main(String[] args) {
        SpringApplication.run(SafeLearnApplication.class, args);
    }
}
