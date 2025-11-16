package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.springframework.http.HttpStatusCode;

import java.time.Duration;
import java.util.Map;

@Component
public class EwelinkAuthClient {

    private final WebClient webClient;
    private final String baseUrl="https://eu-apia.coolkit.cc/v2/";

    public EwelinkAuthClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Logowanie do eWeLink, pobranie tokenu JWT
     */
//    public Mono<String> login(String email, String password, String region) {
//        Map<String, Object> body = Map.of("email", email, "password", password, "version", 8, "ts", System.currentTimeMillis(), "nonce", System.nanoTime(), "region", region);
//
//        return webClient.post().uri("/api/user/login").body(BodyInserters.fromValue(body)).retrieve().onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(Map.class) // Próba pobrania ciała błędu
//                .flatMap(errorBody -> {
//                    String status = clientResponse.statusCode().toString();
//                    System.err.println("Błąd logowania HTTP " + status + ". Ciało błędu: " + errorBody);
//                    // Rzucamy wyjątek, aby Retry mógł go złapać
//                    return Mono.error(new RuntimeException("API error: " + status));
//                })).bodyToMono(String.class).retryWhen(Retry.backoff(3, Duration.ofSeconds(2))).doOnError(e -> {
//            System.err.println("Błąd logowania: " + e.getMessage());
//            e.printStackTrace();
//        });
//                //.map(response -> (String) ((Map) response.get("at")).get("client")); // JWT token
//    }

    public Mono<String> login(String email, String password, String region) {
        Map<String, Object> body = Map.of(
                "email", email,
                "password", password,
                "version", 8,
                "ts", System.currentTimeMillis(),
                "nonce", System.nanoTime(),
                "region", region
        );

        return webClient.post()
                .uri("/user/login")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
               // .header("Authorization", "Sign " + getAuthMac(jsonBody))
                .header("X-Ck-Nonce", Util.getNonce())
                //.header("X-Ck-Appid", APP_ID)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class) // Pobieramy ciało błędu jako String
                                .flatMap(errorBody -> {
                                    String status = clientResponse.statusCode().toString();
                                    System.err.println("Błąd logowania HTTP " + status + ". Ciało błędu: " + errorBody);
                                    return Mono.error(new RuntimeException("API error: " + status + " Body: " + errorBody));
                                })
                )
                .bodyToMono(String.class)
                .map(responseBody -> {
                    // Próba sparsowania JSON i wyciągnięcia tokena
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);
                        Map<String, Object> atMap = (Map<String, Object>) responseMap.get("at");
                        if (atMap != null && atMap.get("client") != null) {
                            return (String) atMap.get("client"); // JWT token
                        } else {
                            throw new RuntimeException("Brak tokena w odpowiedzi: " + responseBody);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Błąd parsowania odpowiedzi: " + responseBody, e);
                    }
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .doOnError(e -> {
                    System.err.println("Błąd logowania: " + e.getMessage());
                    e.printStackTrace();
                });
    }
}
