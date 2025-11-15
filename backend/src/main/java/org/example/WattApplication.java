package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WattApplication {
    public static void main(String[] args) {
        SpringApplication.run(WattApplication.class, args);
        System.out.println("Hello world!");
    }
}