package org.example;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.springframework.http.HttpStatusCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EwelinkAuthClient {

    private static final Logger log = LoggerFactory.getLogger(EwelinkAuthClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    @Value("#{systemEnvironment['app_id']}")
    private String APP_ID;
    @Value("#{systemEnvironment['app_secret']}")
    private String APP_SECRET;
    private final WebClient webClient;
    private final String baseUrl = "https://eu-apia.coolkit.cc/v2/";

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
    public Mono<String> login(String email, String password, String region) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
//        Map<String, Object> body = Map.of(
//                "email", email,
//                "password", password,
//                "version", 8,
//                "ts", System.currentTimeMillis(),
//                "nonce", System.nanoTime(),
//                "region", region
//        );

        String nonce = Util.getNonce();
        long ts = System.currentTimeMillis() / 1000;

        Map<String, Object> body = Map.of(
                "appid", APP_ID,
                "nonce", nonce,
                "ts", ts,
                "version", 8
        );

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Nie można przekształcić JSON login body", e));
        }

        return webClient.post()
                .uri("/open-api/v2/token")
                .header("Content-Type", "application/json")
                .header("X-CK-Appid", APP_ID)
                .header("X-CK-Nonce", nonce)
                .header("X-CK-Timestamp", String.valueOf(ts))
                .header("Authorization", "Sign " + this.getAuthMac(jsonBody))
//                .uri("/user/login")
//                .header("Content-Type", "application/json")
//                .header("Accept", "application/json")
//                .header("Authorization", "Sign " + this.getAuthMac(jsonBody))
//                .header("X-Ck-Nonce", Util.getNonce())
//                .header("X-Ck-Appid", APP_ID)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> {
                                    log.error("❌ Błąd loginu HTTP {} → {}",
                                            clientResponse.statusCode(), error);
                                    return Mono.error(new RuntimeException(
                                            "Login error: " + clientResponse.statusCode() + " Body: " + error));
                                })
                )
                .bodyToMono(String.class)
                // --- Parsowanie odpowiedzi JSON ---
                .map(response -> {
                    try {
                        Map<String, Object> r = mapper.readValue(response, Map.class);
                        Map<String, Object> at = (Map<String, Object>) r.get("at");

                        if (at == null || at.get("client") == null) {
                            log.error("❌ Brak tokena JWT w odpowiedzi: {}", response);
                            throw new RuntimeException("Brak JWT w odpowiedzi API");
                        }

                        String token = (String) at.get("client");
                        log.info("🔑 Otrzymano token JWT (ocenzurowany)");

                        return token;
                    } catch (Exception e) {
                        log.error("❌ Błąd parsowania odpowiedzi JSON podczas loginu: {}", response);
                        throw new RuntimeException("Niepoprawny JSON przy logowaniu", e);
                    }
                })

                // --- Retry 3 razy, rosnące opóźnienie ---
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .onRetryExhaustedThrow((spec, rs) ->
                                new RuntimeException("Login failed after 3 retries"))
                )

                // --- Logowanie finalnego błędu ---
                .doOnError(err -> log.error("❌ Login error: {}", err.getMessage()));
//                .onStatus(HttpStatusCode::isError, clientResponse ->
//                        clientResponse.bodyToMono(String.class) // Pobieramy ciało błędu jako String
//                                .flatMap(errorBody -> {
//                                    String status = clientResponse.statusCode().toString();
//                                    System.err.println("Błąd logowania HTTP " + status + ". Ciało błędu: " + errorBody);
//                                    return Mono.error(new RuntimeException("API error: " + status + " Body: " + errorBody));
//                                })
//                )
//                .bodyToMono(String.class)

//                .map(responseBody -> {
//                    // Próba sparsowania JSON i wyciągnięcia tokena
//                    try {
//                        ObjectMapper mapper = new ObjectMapper();
//                        Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);
//                        Map<String, Object> atMap = (Map<String, Object>) responseMap.get("at");
//                        if (atMap != null && atMap.get("client") != null) {
//                            return (String) atMap.get("client"); // JWT token
//                        } else {
//                            throw new RuntimeException("Brak tokena w odpowiedzi: " + responseBody);
//                        }
//                    } catch (Exception e) {
//                        throw new RuntimeException("Błąd parsowania odpowiedzi: " + responseBody, e);
//                    }
//                })
//                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
//                .doOnError(e -> {
//                    System.err.println("Błąd logowania: " + e.getMessage());
//                    e.printStackTrace();
//                });
    }

    private String getAuthMac(String data) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {

        Mac sha256_HMAC = null;

        byte[] byteKey = APP_SECRET.getBytes("UTF-8");
        final String HMAC_SHA256 = "HmacSHA256";
        sha256_HMAC = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_SHA256);
        sha256_HMAC.init(keySpec);
        byte[] mac_data = sha256_HMAC.
                doFinal(data.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(mac_data);


    }
}
