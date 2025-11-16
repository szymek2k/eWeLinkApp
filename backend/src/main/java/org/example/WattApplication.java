package org.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class WattApplication implements CommandLineRunner {

    private final WattService wattService;

    public WattApplication(WattService wattService) {
        this.wattService = wattService;
    }

    public static void main(String[] args) {
        SpringApplication.run(WattApplication.class, args);
        System.out.println("Hello world!");
    }

    @Override
    public void run(String... args) {
        System.out.println("Uruchamianie pobierania danych Watt...");

        Flux<WattUsage> flux = wattService.pollWattReactive();
        flux.subscribe(
                data -> System.out.println("Otrzymane dane: " + data),
                error -> System.err.println("Błąd: " + error.getMessage())
        );

        // Zatrzymanie aplikacji nie blokuje pobierania - reactive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}