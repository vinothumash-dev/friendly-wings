package com.friendlywings.automation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FriendlyWingsAutomationApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendlyWingsAutomationApplication.class, args);
    }
}
