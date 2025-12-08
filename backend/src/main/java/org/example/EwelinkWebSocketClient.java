package org.example;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Component
public class EwelinkWebSocketClient {

    private WebSocketSession session;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String DEVICE_ID = "";

    private static final String APP_ID = "";

    public Mono<Void> connect(String accessToken, String apikey) {

        String url = "wss://eu-pconnect3.coolkit.cc:8080/api/ws";

        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

        return client.execute(URI.create(url), session -> {

            this.session = session;

            System.out.println("WS: Połączono z eWeLink");
            sendUserOnline(accessToken);

            // Po połączeniu poczekaj sekundę — eWeLink tak lubi
            Mono.delay(java.time.Duration.ofSeconds(1))
                    .doOnSuccess(t -> queryDevice(DEVICE_ID, apikey))
                    .subscribe();

            return session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(this::handleMsg)
                    .then();
        });
    }

    private void sendUserOnline(String at) {
        ObjectNode msg = mapper.createObjectNode();

        msg.put("action", "user.online");
        msg.put("userAgent", "app");
        msg.put("version", 8);
        msg.put("appid", APP_ID);
        msg.put("ts", System.currentTimeMillis() / 1000);
        msg.put("nonce", UUID.randomUUID().toString());
        msg.put("sequence", String.valueOf(System.currentTimeMillis()));
        msg.put("at", at);

        sendJson(msg);

        System.out.println("WS: Wysłano user.online");
    }

    public void queryDevice(String deviceId, String apikey) {
        ObjectNode msg = mapper.createObjectNode();

        msg.put("action", "query");
        msg.put("userAgent", "app");
        msg.put("from", "app");
        msg.put("deviceid", deviceId);
        msg.put("apikey", apikey);
        msg.put("sequence", String.valueOf(System.currentTimeMillis()));

        // Pusta lista = pobiera WSZYSTKIE parametry urządzenia
        ArrayNode params = mapper.createArrayNode();
        msg.set("params", params);

        sendJson(msg);

        System.out.println("WS: Wysłano query dla urządzenia " + deviceId);
    }

    private void sendJson(JsonNode json) {
        if (session == null) return;

        session.send(Mono.just(session.textMessage(json.toString())))
                .subscribe();
    }

    private void handleMsg(String raw) {
        try {
            JsonNode msg = mapper.readTree(raw);

            // Każda zmiana urządzenia to: action=update
            if (msg.has("action") &&
                    msg.get("action").asText().equals("update")) {

                JsonNode params = msg.get("params");

                if (params != null && params.has("power")) {
                    double watts = params.get("power").asDouble();
                    System.out.println("⚡ Moc urządzenia: " + watts + " W");
                }

                System.out.println("WS→ " + msg.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}