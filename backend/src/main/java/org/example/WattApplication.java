package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@SpringBootApplication
public class WattApplication implements CommandLineRunner {
    Logger logger = LoggerFactory.getLogger(WattApplication.class);
    @Value("#{systemEnvironment['ewelink_email']}")
    private String email;

    @Value("#{systemEnvironment['ewelink_password']}")
    private String password;

    @Value("#{systemEnvironment['ewelink_region']}")
    private String region;
    private final WattService wattService;

    private final EwelinkAuthClient ewelinkAuthClient;

    public WattApplication(WattService wattService, EwelinkAuthClient ewelinkAuthClient) {
        this.wattService = wattService;
        this.ewelinkAuthClient = ewelinkAuthClient;
    }

    public static void main(String[] args) {
         SpringApplication.run(WattApplication.class, args);
        //SpringApplication.run(WattApplication.class, args);
        System.out.println("Hello world!");
    }


//    @Override
//    public void run(String... args) throws Exception {
//        this.login();
//    }

    @Override
    public void run(String... args) {
//        ewelinkAuthClient.exchangeCodeForTokensAndConnect("")
//                .thenReturn("<html><body><h1>Autoryzacja udana!</h1><p>Tokeny pobrano i rozpoczęto połączenie WebSocket.</p></body></html>")
//                .onErrorResume(e -> {
//                    System.err.println("Błąd w trakcie autoryzacji: " + e.getMessage());
//                    return Mono.just("<html><body><h1>Błąd autoryzacji</h1><p>Wystąpił błąd: " + e.getMessage() + "</p></body></html>");
//                }).subscribe(v -> System.out.println("Odpowiedz " + v));

    }
}