package org.example;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SignatureHelper {

    public static String calculatePostSignature(String jsonBody, String appSecret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secretKey);

            byte[] hash = sha256_HMAC.doFinal(jsonBody.getBytes(StandardCharsets.UTF_8));

            // Konwersja na Base64
            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas generowania sygnatury HMAC-SHA256", e);
        }
    }

    public static String sha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);

            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // hex lowercase output
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Cannot compute HMAC-SHA256 signature", e);
        }
    }
}
