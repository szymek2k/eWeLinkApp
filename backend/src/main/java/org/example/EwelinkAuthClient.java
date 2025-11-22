package org.example;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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
import java.time.LocalDateTime;
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
    private final String baseUrl = "https://eu-apia.coolkit.cc";

    // Zmienne stanu tokenów
    private volatile String currentAccessToken = null;
    private volatile String currentRefreshToken = null;
    private volatile LocalDateTime tokenExpiryTime = null; // Do

    //    @Value("${ewelink.redirect.uri}")
//    private String redirectUri=
    public EwelinkAuthClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl(baseUrl).build();

//        authenticateOrRefresh().subscribe(
//                token -> log.info("Pomyślnie uzyskano początkowy token dostępu."),
//                error -> log.error("Błąd autoryzacji eWeLink: {}", error.getMessage())
//        );
    }

    private void saveTokenData(TokenResponse res) {
        this.currentAccessToken = res.access_token();
        this.currentRefreshToken = res.refresh_token();
        // Ustaw czas wygaśnięcia na 1 minutę przed faktycznym wygaśnięciem
        this.tokenExpiryTime = LocalDateTime.now().plusSeconds(res.expires_in()).minusMinutes(1);

        // W produkcji: Zapisz currentRefreshToken i tokenExpiryTime do trwałego magazynu!
        log.warn("💾 ZAPISANO NOWY RT: {} | Wygasa: {}", this.currentRefreshToken, this.tokenExpiryTime);
    }


    /**
     * Główna metoda zarządzająca autoryzacją.
     * Używa zapisanego Refresh Tokena, jeśli istnieje, lub wymienia kod na tokeny, jeśli podano kod.
     *
     * @param authorizationCode Kod autoryzacyjny (wymagany tylko raz, null po pierwszym użyciu).
     */
    public Mono<String> authenticateOrRefresh(String authorizationCode) {

        // 1. Jeśli mamy kod autoryzacyjny (pierwsze logowanie), użyj kodu.
        if (authorizationCode != null && !authorizationCode.isBlank()) {
            log.info("Tryb: Inicjalna autoryzacja - wymiana kodu.");
            return exchangeCodeForTokens(authorizationCode);
        }

        // 2. Tryb: Odświeżenie (jeśli mamy RT)
        if (this.currentRefreshToken != null) {
            return requestAccessTokenViaRefreshToken(this.currentRefreshToken);
        }

// 3. Błąd
        log.error("Nie można się zalogować: Brak kodu autoryzacyjnego i zapisanego Refresh Tokena.");
        return Mono.error(new IllegalStateException("Wymagana autoryzacja inicjalna."));
    }

    /**
     * Używana tylko raz, aby wymienić kod autoryzacyjny na access_token i refresh_token.
     */
    public Mono<String> exchangeCodeForTokens(String authorizationCode) {
        log.info("Rozpoczęcie wymiany kodu autoryzacyjnego na tokeny za pomocą POST /v2/user/oauth/token");

        return webClient.post()
                .uri("/v2/user/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new InitialTokenRequest(APP_ID, APP_SECRET, authorizationCode))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .flatMap(res -> {
                    if (res.error() == 0) {
                        log.info("Pomyślnie uzyskano Access Token i Refresh Token. Ważny przez {} sekund.", res.expires_in());
                        saveTokenData(res);

                        // TUTAJ: ZAPISZ currentRefreshToken do trwałego magazynu (np. bazy danych)!
                        // Ponieważ ten token jest potrzebny do cyklicznego odświeżania.
                        log.warn("ZAPISZ NOWY REFRESH TOKEN: {}", this.currentRefreshToken);

                        return Mono.just(this.currentAccessToken);
                    } else {
                        log.error("Błąd API eWeLink podczas wymiany kodu na tokeny: {} (kod: {})", res.msg(), res.error());
                        return Mono.error(new RuntimeException("Nieudana wymiana kodu eWeLink."));
                    }
                })
                .doOnError(e -> log.error("Błąd komunikacji podczas wymiany kodu: {}", e.getMessage()));
    }

    public Mono<String> requestAccessTokenViaRefreshToken(String refreshToken) {
        log.info("Rozpoczęcie procesu odświeżania tokena dostępu");

        // Zabezpieczenie: Logowanie tokenów tylko na poziomie DEBUG
        log.debug("Używany refreshToken: {}", "-------");

        // Body requestu zgodne z dokumentacją eWeLink API
        // Zwróć uwagę, że nie używamy tutaj hasła ani username!
        Mono<TokenResponse> responseMono = webClient.post()
                .uri("/v2/user/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TokenRefreshRequest(APP_ID, refreshToken))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                // --- Retry 3 razy, rosnące opóźnienie ---
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .onRetryExhaustedThrow((spec, rs) ->
                                new RuntimeException("Login failed after 3 retries"))
                );

        return responseMono
                .flatMap(res -> {
                    if (res.error() == 0) {
                        this.currentAccessToken = res.access_token();
                        log.info("Nowy token dostępu uzyskany. Ważny przez {} sekund.", res.expires_in());
                        return Mono.just(this.currentAccessToken);
                    } else {
                        log.error("Błąd API eWeLink podczas odświeżania tokena: {} (kod: {})", res.msg(), res.error());
                        return Mono.error(new RuntimeException("Nieudane odświeżenie tokena eWeLink"));
                    }
                });
    }

    /**
     * Zwraca aktualny Access Token.
     */
    public String getAccessToken() {
        if (currentAccessToken == null || (tokenExpiryTime != null && LocalDateTime.now().isAfter(tokenExpiryTime))) {
            throw new IllegalStateException("Access Token wygasł lub jest nieaktywny. Wymagane jest odświeżenie lub autoryzacja.");
        }
        return currentAccessToken;
    }

    // 1. Odpowiedź z Tokenami (zarówno z wymiany kodu, jak i odświeżania)
    record TokenResponse(
            int error,
            String msg,
            String access_token,
            String refresh_token,
            long expires_in
    ) {
    }

    // 3. Żądanie odświeżenia Tokena (POST /v2/user/refresh)
    record TokenRefreshRequest(
            String appid,
            String refreshToken
    ) {
    }

    // 2. Żądanie wymiany Kodu Autoryzacyjnego na Tokeny (POST /v2/user/oauth/token)
    record InitialTokenRequest(
            String appid,
            String appsecret,
            String code
    ) {
    }
}
