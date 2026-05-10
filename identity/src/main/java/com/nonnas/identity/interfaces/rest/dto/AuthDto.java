package com.nonnas.identity.interfaces.rest.dto;

import com.nonnas.identity.application.auth.LoginResult;
import com.nonnas.identity.application.auth.TokenPair;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.Usuario;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

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

    public record UsuarioResumo(
            UUID id,
            String nome,
            String email,
            Perfil perfil,
            UUID filialId
    ) {
        public static UsuarioResumo from(Usuario u) {
            return new UsuarioResumo(
                    u.id().value(),
                    u.nome(),
                    u.email().value(),
                    u.perfil(),
                    u.filialId().map(f -> f.value()).orElse(null)
            );
        }
    }

    public record LoginResponse(
            String accessToken,
            Instant accessExpiresAt,
            String refreshToken,
            Instant refreshExpiresAt,
            String tokenType,
            UsuarioResumo usuario
    ) {
        public static LoginResponse from(LoginResult result) {
            TokenPair p = result.tokens();
            return new LoginResponse(
                    p.accessToken(),
                    p.accessExpiresAt(),
                    p.refreshToken(),
                    p.refreshExpiresAt(),
                    "Bearer",
                    UsuarioResumo.from(result.usuario())
            );
        }
    }

    private AuthDto() {}
}
