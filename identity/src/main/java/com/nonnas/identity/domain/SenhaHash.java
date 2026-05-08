package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.util.Objects;

/**
 * Wrapper imutável para o hash BCrypt da senha. Tem dois objetivos:
 * <ul>
 *   <li>Tipo distinto (não é qualquer String) — evita passar plaintext por engano.</li>
 *   <li>{@link #toString()} mascara o valor — defesa em profundidade contra logs acidentais.</li>
 * </ul>
 *
 * <p>Validação aceita os três prefixos canônicos do BCrypt: {@code $2a$},
 * {@code $2b$}, {@code $2y$}.
 */
public record SenhaHash(String value) {

    public SenhaHash {
        Objects.requireNonNull(value, "SenhaHash value must not be null");
        if (!value.startsWith("$2a$") && !value.startsWith("$2b$") && !value.startsWith("$2y$")) {
            throw new ValidationException("Hash de senha inválido: não tem prefixo BCrypt");
        }
    }

    public static SenhaHash of(String value) {
        return new SenhaHash(value);
    }

    @Override
    public String toString() {
        return "SenhaHash[***]";
    }
}
