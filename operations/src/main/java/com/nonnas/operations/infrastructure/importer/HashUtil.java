package com.nonnas.operations.infrastructure.importer;

import com.nonnas.sharedkernel.ValidationException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class HashUtil {

    private HashUtil() {}

    /** SHA-256 hex (64 chars lowercase). Identidade lógica da planilha para idempotência. */
    static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ValidationException("SHA-256 indisponível: " + e.getMessage());
        }
    }
}
