package org.example;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class EwelinkDeviceClient {

    private final WebClient webClient;

    public EwelinkDeviceClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://eu-apia.coolkit.cc").build();
    }

    /**
     * Pobiera listę urządzeń użytkownika
     */
//    public Mono<String> getDevices() {
//        return webClient.get()
//                .uri("/v2/device")
//                .header("Authorization", "Bearer " + token)
//                .retrieve()
//                .bodyToMono(String.class);
//    }

    /**
     * Pobiera szczegóły konkretnego urządzenia
     */
//    public Mono<String> getDeviceDetails(String deviceId) {
//        return webClient.get()
//                .uri("/v2/device/{id}", deviceId)
//                .header("Authorization", "Bearer " + token)
//                .retrieve()
//                .bodyToMono(String.class);
//    }

    /**
     * Pobiera dane z licznika mocy (watt) z urządzenia typu Switch / Plug
     */
    public Mono<WattUsage> getWattUsage(String deviceId, String token) {
        return webClient.get()
                .uri("/v2/device/{id}/watt", deviceId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(WattUsage.class);
    }
}
