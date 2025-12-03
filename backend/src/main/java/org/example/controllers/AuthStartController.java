package org.example.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@RestController
public class AuthStartController {

    // Wstrzyknięcie wymaganych parametrów
    //@Value("#{systemEnvironment['app_id']}")
    private String appId = "";
    private String app_secret = "";

    @Value("${ewelink.redirect.uri}")
    private String redirectUri="http://localhost:8080/auth/return";

    /**
     * Reaktywny endpoint startowy: http://localhost:8080/auth/login
     * Zwraca Mono<String> z instrukcją przekierowania do eWeLink.
     * <p>
     * https://coolkit-technologies.github.io/eWeLink-API/#/en/OAuth2.0?id=v2-interface-signature-rules
     * Rozdział 'Authorization Page Description'
     * Wywoływane z frontu http://localhost:4200/
     */
    @GetMapping("/auth/login")
    public Mono<String> login() {
        // Logika jest synchroniczna, ale opakowujemy wynik w Mono
        // (nie wykonujemy blokującej operacji I/O)

        // 1. Ustawienie hosta API (np. https://eu-apia.coolkit.cc)
        String host = "https://c2c-open.coolkit.cc";

        long seq = System.currentTimeMillis();
        String nonce = "hdicm290";

        // 2. Generowanie unikalnego stanu (state)
        String state = UUID.randomUUID().toString();

        String message = appId + "_" + seq;
        Mac sha256_HMAC;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(app_secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String authorization = Base64.getEncoder().encodeToString(
                sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8))
        );

        String url = UriComponentsBuilder.fromHttpUrl("https://c2ccdn.coolkit.cc/oauth/index.html")
                .queryParam("clientId", appId)
                .queryParam("authorization", authorization)
                .queryParam("seq", seq)
                .queryParam("redirectUrl", redirectUri)
                .queryParam("grantType", "authorization_code")
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("showQRCode", "false")
                .build()
                .toUriString();

        return Mono.just("redirect:" + url);
    }
}