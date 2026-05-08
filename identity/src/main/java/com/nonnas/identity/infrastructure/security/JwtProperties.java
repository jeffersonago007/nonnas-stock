package com.nonnas.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "nonnas.security.jwt")
public record JwtProperties(
        String secret,
        Duration accessTtl,
        Duration refreshTtl,
        String issuer
) {
    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "nonnas.security.jwt.secret deve ter pelo menos 32 caracteres (256 bits para HS256)");
        }
        if (accessTtl == null) accessTtl = Duration.ofMinutes(15);
        if (refreshTtl == null) refreshTtl = Duration.ofDays(7);
        if (issuer == null || issuer.isBlank()) issuer = "nonnas-stock";
    }
}
