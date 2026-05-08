package com.nonnas.identity.application.auth;

import java.time.Instant;

public record TokenPair(
        String accessToken,
        Instant accessExpiresAt,
        String refreshToken,
        Instant refreshExpiresAt
) {}
