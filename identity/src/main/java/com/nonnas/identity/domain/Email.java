package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Endereço de email validado por padrão simplificado RFC 5322 e normalizado
 * para lowercase. Comparação e busca são case-insensitive porque a maioria
 * dos provedores de email trata local-part como case-insensitive na prática.
 */
public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    public Email {
        Objects.requireNonNull(value, "Email value must not be null");
        String normalized = value.trim().toLowerCase();
        if (!PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("Email inválido: " + value);
        }
        value = normalized;
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
