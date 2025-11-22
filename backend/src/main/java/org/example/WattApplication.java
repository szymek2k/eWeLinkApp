package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class WattApplication {
    Logger logger = LoggerFactory.getLogger(WattApplication.class);
    @Value("#{systemEnvironment['ewelink_email']}")
    private String email;

    @Value("#{systemEnvironment['ewelink_password']}")
    private String password;

    @Value("#{systemEnvironment['ewelink_region']}")
    private String region;
    private final WattService wattService;

    public WattApplication(WattService wattService) {
        this.wattService = wattService;
    }

    public static void main(String[] args) {
        // SpringApplication.run(WattApplication.class, args);
        SpringApplication.run(WattApplication.class, args);
        System.out.println("Hello world!");
    }


//    @Override
//    public void run(String... args) throws Exception {
//        this.login();
//    }

//    @Override
//    public void run(String... args) {
//        System.out.println("Uruchamianie pobierania danych Watt...");
//
//        Flux<WattUsage> flux = wattService.pollWattReactive();
//        flux.subscribe(
//                data -> System.out.println("Otrzymane dane: " + data),
//                error -> System.err.println("Błąd: " + error.getMessage())
//        );
//
//        // Zatrzymanie aplikacji nie blokuje pobierania - reactive
//        try {
//            Thread.currentThread().join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
}