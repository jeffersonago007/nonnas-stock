package com.nonnas.identity.interfaces.rest.dto;

import com.nonnas.identity.application.auth.TokenPair;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class AuthDto {

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String senha
    ) {}

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}

    public record LogoutRequest(
            String refreshToken    // opcional — ausente revoga apenas o access token
    ) {}

    public record TokenResponse(
            String accessToken,
            Instant accessExpiresAt,
            String refreshToken,
            Instant refreshExpiresAt,
            String tokenType
    ) {
        public static TokenResponse from(TokenPair pair) {
            return new TokenResponse(
                    pair.accessToken(),
                    pair.accessExpiresAt(),
                    pair.refreshToken(),
                    pair.refreshExpiresAt(),
                    "Bearer"
            );
        }
    }

    private AuthDto() {}
}
