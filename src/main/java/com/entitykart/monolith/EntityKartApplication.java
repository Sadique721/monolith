package com.entitykart.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class EntityKartApplication {
    public static void main(String[] args) {
        SpringApplication.run(EntityKartApplication.class, args);
        System.out.println("EntityKart Application Started Successfully");
    }
}
