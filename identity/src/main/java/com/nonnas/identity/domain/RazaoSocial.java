package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.util.Objects;

public record RazaoSocial(String value) {

    public RazaoSocial {
        Objects.requireNonNull(value, "RazaoSocial value must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("Razão social é obrigatória");
        }
        if (trimmed.length() > 255) {
            throw new ValidationException("Razão social não pode exceder 255 caracteres");
        }
        value = trimmed;
    }

    public static RazaoSocial of(String value) {
        return new RazaoSocial(value);
    }
}
