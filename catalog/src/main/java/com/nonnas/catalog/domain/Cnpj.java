package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.util.Objects;

/**
 * CNPJ duplicado pragmaticamente do módulo identity. T10 promove para
 * shared-kernel quando outros módulos também precisarem (ou para um
 * módulo {@code br-commons}).
 */
public record Cnpj(String value) {

    public Cnpj {
        Objects.requireNonNull(value, "CNPJ value must not be null");
        String cleaned = value.replaceAll("\\D", "");
        if (!isValid(cleaned)) {
            throw new ValidationException("CNPJ inválido: " + value);
        }
        value = cleaned;
    }

    public static Cnpj of(String value) {
        return new Cnpj(value);
    }

    public String formatted() {
        return "%s.%s.%s/%s-%s".formatted(
                value.substring(0, 2),
                value.substring(2, 5),
                value.substring(5, 8),
                value.substring(8, 12),
                value.substring(12, 14)
        );
    }

    private static boolean isValid(String cnpj) {
        if (cnpj.length() != 14) return false;
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int dv1 = computeCheckDigit(cnpj, w1, 12);
        if (dv1 != Character.getNumericValue(cnpj.charAt(12))) return false;
        return computeCheckDigit(cnpj, w2, 13) == Character.getNumericValue(cnpj.charAt(13));
    }

    private static int computeCheckDigit(String cnpj, int[] weights, int len) {
        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights[i];
        }
        int rem = sum % 11;
        return rem < 2 ? 0 : 11 - rem;
    }
}
