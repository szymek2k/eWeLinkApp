package org.example.dto;

import lombok.Data;
import org.example.dto.TokenData;

// Główne DTO dla odpowiedzi API eWeLink
@Data
public class TokenResponse {
    private int error;
    private String msg;
    private TokenData data;
}
