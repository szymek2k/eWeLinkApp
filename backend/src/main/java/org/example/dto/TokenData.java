package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// Dane tokenów i użytkownika
@Data
public class TokenData {
    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("region")
    private String region;

    @JsonProperty("expiresIn")
    private long expiresIn; // Czas ważności w sekundach

    @JsonProperty("userId")
    private String userId;
}
