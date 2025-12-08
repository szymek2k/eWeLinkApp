package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.Random;

@Service
public class EwelinkWebSocketService {

    private static final String APP_ID = "";
    private static final String APP_SECRET = "";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void connect(String accessToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            client.newWebSocketBuilder()
                    .buildAsync(URI.create("wss://eu-pconnect3.coolkit.cc:8080/api/ws"),
                            new WebSocket.Listener() {

                                @Override
                                public void onOpen(WebSocket webSocket) {
                                    System.out.println("WS: Połączono z eWeLink");
                                    sendUserOnline(webSocket, accessToken);
                                    WebSocket.Listener.super.onOpen(webSocket);
                                }

                                @Override
                                public java.util.concurrent.CompletionStage<?> onText(
                                        WebSocket webSocket,
                                        CharSequence data,
                                        boolean last
                                ) {
                                    handleDeviceUpdate(data.toString());
                                    return WebSocket.Listener.super.onText(webSocket, data, last);
                                }
                            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendUserOnline(WebSocket ws, String accessToken) {
        try {
            long ts = Instant.now().getEpochSecond();
            String nonce = String.valueOf(new Random().nextInt(99999999));
            String sequence = String.valueOf(ts);
            String sign = hmacSha256(ts + nonce + accessToken, APP_SECRET);

            String payload = String.format(
                    "{\"action\":\"user.online\",\"userAgent\":\"app\",\"version\":8," +
                            "\"appid\":\"%s\",\"ts\":\"%d\",\"nonce\":\"%s\",\"sequence\":\"%s\"," +
                            "\"at\":\"%s\",\"sign\":\"%s\"}",
                    APP_ID, ts, nonce, sequence, accessToken, sign
            );

            ws.sendText(payload, true);
            System.out.println("WS: Wysłano user.online");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleDeviceUpdate(String json) {
        try {
            JsonNode root = mapper.readTree(json);

            if (!root.has("params")) return; // nie dotyczy urządzenia

            JsonNode params = root.get("params");

            Double watt = null;

            // 🔥 Obsługa różnych typów urządzeń:

            if (params.has("power")) {
                watt = params.get("power").asDouble();
            }

            if (params.has("watt")) {
                watt = params.get("watt").asDouble();
            }

            if (params.has("current") && params.has("voltage")) {
                double current = params.get("current").asDouble();
                double voltage = params.get("voltage").asDouble();
                watt = current * voltage;
            }

            if (watt != null) {
                System.out.println("⚡ Zmiana mocy urządzenia: " + watt + " W");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
        return bytesToHex(mac.doFinal(data.getBytes()));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

