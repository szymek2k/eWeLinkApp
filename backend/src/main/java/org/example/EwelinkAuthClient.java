package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.reactive.socket.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.TokenData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class EwelinkAuthClient {

    private static final Logger log = LoggerFactory.getLogger(EwelinkAuthClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    // @Value("#{systemEnvironment['app_id']}")
    private String APP_ID = "";
    //@Value("#{systemEnvironment['app_secret']}")
    private String APP_SECRET = "";
    private final WebClient webClient;
    private final String baseUrl = "https://eu-apia.coolkit.cc";

    private final ReactorNettyWebSocketClient wsClient = new ReactorNettyWebSocketClient();

    // current tokens (atomic container for thread-safety)
    private final AtomicReference<OAuthData> currentTokens = new AtomicReference<>();
    private Disposable scheduledRefresh = null;

    private String redirectUri = "http://localhost:8080/auth/return";

    //    @Value("${ewelink.redirect.uri}")
//    private String redirectUri=
    public EwelinkAuthClient() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // EXCHANGE authorization code for tokens and then connect WS
    public Mono<OAuthData> exchangeCodeForTokensAndConnect(String code) {
        System.out.println("-> Rozpoczynam wymianę kodu autoryzacyjnego na tokeny...");
        return performTokenExchange("authorization_code", code)
                .flatMap(data -> {
                    currentTokens.set(data);
                    System.out.println("-> Sukces. accessToken present: " + (data.getAccessToken() != null));
                    scheduleRefreshIfNeeded(data);

                    System.out.println("exchangeCodeForTokensAndConnect " + data.toString());
                    String wsUrl = getWebSocketUrlForRegion("eu"); // v2 nie zwraca region - domyślnie eu
                    return connectAndAuthenticateWebSocket(wsUrl, data).thenReturn(data);
                });
    }

    // Connect and authenticate WS using v2 userOnline message
    private Mono<Void> connectAndAuthenticateWebSocket(String wsUrl, OAuthData data) {
        System.out.println("-> Łączenie z WebSocket: " + wsUrl);

        return wsClient.execute(
                URI.create(wsUrl),
                session -> {

                    // --- BUILD user.online ---
                    long ts = System.currentTimeMillis() / 1000;
                    String tsStr = String.valueOf(ts);
                    String nonce = UUID.randomUUID().toString().substring(0, 8);

                    ObjectNode online = mapper.createObjectNode();
                    online.put("action", "user.online");
                    online.put("userAgent", "app");
                    online.put("version", 8);
                    online.put("appid", APP_ID);
                    online.put("ts", tsStr);
                    online.put("nonce", nonce);
                    online.put("sequence", tsStr);
                    online.put("at", data.getAccessToken());

                    // --- CHANGED: poprawiony sign ---
                    String signPayload = APP_ID + data.getAccessToken() + nonce + tsStr;
                    String sign = SignatureHelper.sha256Hex(APP_SECRET, signPayload); // ADDED: correct order
                    online.put("sign", sign);

                    String onlineStr;
                    try {
                        onlineStr = mapper.writeValueAsString(online);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }

                    System.out.println("-> WS SEND user.online: " + onlineStr);

                    // --- SEND user.online ---
                    Mono<Void> sendOnline = session.send(
                            Mono.just(session.textMessage(onlineStr))
                    );

                    // --- PING ---
                    Disposable ping = Flux.interval(Duration.ofSeconds(10))
                            .flatMap(tick -> {
                                long nowTs = System.currentTimeMillis() / 1000;
                                String nowTsStr = String.valueOf(nowTs);
                                String pingMsg = "{ \"action\":\"ping\", \"ts\": " + nowTs + ", \"sequence\":\"" + nowTsStr + "\" }";
                                return session.send(Mono.just(session.textMessage(pingMsg)));
                            })
                            .subscribe();

                    // --- RECEIVE + SEND query.all ---
                    Mono<Void> receive = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(msg -> {
                                System.out.println("WS RECV: " + msg);

                                // czekamy na user.online OK
                                if (msg.contains("\"error\":0") && msg.contains("user.online")) {

                                    try {
                                        long ts2 = System.currentTimeMillis() / 1000;
                                        String ts2Str = String.valueOf(ts2);
                                        String nonce2 = UUID.randomUUID().toString().substring(0, 8);

                                        ObjectNode query = mapper.createObjectNode();
                                        query.put("action", "query.all");
                                        query.put("userAgent", "app");
                                        query.put("version", 8);
                                        query.put("appid", APP_ID);
                                        query.put("ts", ts2Str);
                                        query.put("nonce", nonce2);
                                        query.put("sequence", ts2Str);
                                        query.put("apikey", data.getApikey());

                                        // --- CHANGED: sign dla query.all ---
                                        String sign2Payload = APP_ID + data.getApikey() + nonce2 + ts2Str;
                                        String sign2 = SignatureHelper.sha256Hex(APP_SECRET, sign2Payload);
                                        query.put("sign", sign2);

                                        String queryStr = mapper.writeValueAsString(query);

                                        System.out.println("-> WS SEND query.all: " + queryStr);

                                        session.send(Mono.just(session.textMessage(queryStr))).subscribe();

                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            })
                            .doFinally(sig -> ping.dispose())
                            .then();

                    return Mono.zip(sendOnline, receive).then();
                }
        );
    }

    private String buildWsAuthMessage(String accessToken) {
        long ts = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().substring(0, 8);
        String sequence = String.valueOf(ts);

        // sign = sha256(appId + at + nonce + ts)
        String toSign = APP_ID + accessToken + nonce + ts;
        String sign = SignatureHelper.sha256Hex(APP_SECRET, toSign);

        Map<String, Object> msg = new HashMap<>();
        msg.put("action", "user.online");
        msg.put("appid", APP_ID);
        msg.put("version", 8);
        msg.put("ts", ts);
        msg.put("at", accessToken);
        msg.put("userAgent", "app");
        msg.put("nonce", nonce);
        msg.put("sequence", sequence);
        msg.put("sign", sign);

        try {
            return mapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // simple mapping to eu WS endpoint for v2 (region not returned)
    private String getWebSocketUrlForRegion(String region) {
// default to eu-pconnect3
        return String.format("wss://%s-pconnect3.coolkit.cc:8080/api/ws", region);
    }


    // REFRESH token and reconnect WS
    public Mono<Void> refreshTokenAndReconnect() {
        OAuthData tokens = currentTokens.get();
        if (tokens == null || tokens.getRefreshToken() == null) {
            return Mono.error(new IllegalStateException("Brak tokena odświeżania do użycia. Wymagana ponowna autoryzacja."));
        }


        System.out.println("-> Rozpoczynam odświeżanie tokena...");


        return performTokenExchange("refresh_token", tokens.getRefreshToken())
                .flatMap(newData -> {
                    currentTokens.set(newData);
                    System.out.println("-> Token odświeżony pomyślnie.");
                    scheduleRefreshIfNeeded(newData);


                    String wsUrl = getWebSocketUrlForRegion("eu");
                    return connectAndAuthenticateWebSocket(wsUrl, newData);
                });
    }

    // schedule refresh slightly before expiration
    private void scheduleRefreshIfNeeded(OAuthData data) {
        if (scheduledRefresh != null && !scheduledRefresh.isDisposed()) {
            scheduledRefresh.dispose();
        }


        long now = System.currentTimeMillis();
        long atExpiry = data.getAtExpiredTime();
        long delayMs = atExpiry - now - 30_000; // refresh 30s before expiry
        if (delayMs <= 0) {
            delayMs = 1_000; // immediate if already expired or very close
        }


        System.out.println("-> Zarezerwowanie odświeżania tokena za " + delayMs + " ms");


        scheduledRefresh = Mono.delay(Duration.ofMillis(delayMs))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(ignored -> {
                    System.out.println("-> Wywołanie odświeżania tokena z scheduler");
                    return refreshTokenAndReconnect();
                })
                .subscribe(
                        v -> {
                        },
                        err -> System.err.println("Błąd w czasie odświeżania: " + err.getMessage())
                );
    }


    // perform token exchange with correct JSON field names for v2
    // perform token exchange with correct JSON field names for v2
    private Mono<OAuthData> performTokenExchange(String grantType, String codeOrRefreshToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("grantType", grantType);
        body.put("appId", APP_ID);
        body.put("appSecret", APP_SECRET);


        if ("authorization_code".equals(grantType)) {
            body.put("code", codeOrRefreshToken);
            body.put("redirectUrl", redirectUri);
        } else if ("refresh_token".equals(grantType)) {
            body.put("refreshToken", codeOrRefreshToken);
        }


        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(body);
        } catch (Exception ex) {
            return Mono.error(new RuntimeException("Błąd serializacji JSON", ex));
        }


        String nonce = UUID.randomUUID().toString().substring(0, 8);
        String signature = SignatureHelper.calculatePostSignature(jsonBody, APP_SECRET);
        String authorizationHeader = "Sign " + signature;


        return webClient.post()
                .uri("/v2/user/oauth/token")
                .header("X-CK-Appid", APP_ID)
                .header("X-CK-Nonce", nonce)
                .header("Authorization", authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .flatMap(resp -> {
                    if (resp.getError() == 0 && resp.getData() != null) {
                        OAuthData data = resp.getData();
                        return Mono.just(data);
                    } else {
                        String err = String.format("Błąd wymiany tokenu (grantType: %s): Kod: %d, Wiadomość: %s",
                                grantType, resp.getError(), resp.getMsg());
                        return Mono.error(new RuntimeException(err));
                    }
                });
    }
}