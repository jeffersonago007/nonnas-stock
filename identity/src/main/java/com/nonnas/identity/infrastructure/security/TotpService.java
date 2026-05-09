package com.nonnas.identity.infrastructure.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;

/**
 * TOTP (RFC 6238) — secret de 20 bytes, código 6 dígitos, janela 30s.
 * Apenas JDK (HmacSHA1). Sem deps externas.
 *
 * <p>Para gerar QR Code no browser, devolvemos a URI {@code otpauth://}
 * — apps autenticadores (Google Authenticator, Authy, 1Password, etc.)
 * conhecem esse formato e o frontend renderiza o QR localmente com qualquer
 * lib JS pequena.
 */
@Service
public class TotpService {

    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int SECRET_BYTES = 20;
    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private final SecureRandom rng = new SecureRandom();
    private final Clock clock;

    public TotpService(Clock clock) {
        this.clock = clock;
    }

    public String generateSecretBase32() {
        byte[] secret = new byte[SECRET_BYTES];
        rng.nextBytes(secret);
        return base32Encode(secret);
    }

    public String otpauthUri(String secretBase32, String userEmail, String issuer) {
        String label = URLEncoder.encode(issuer + ":" + userEmail, StandardCharsets.UTF_8);
        String issuerEncoded = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + secretBase32
                + "&issuer=" + issuerEncoded
                + "&algorithm=SHA1"
                + "&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    /**
     * Calcula o código TOTP da janela corrente. Permite que testes
     * confirmem 2FA gerando localmente o código que o app autenticador
     * geraria no mesmo momento.
     */
    public String computeCurrentCode(String secretBase32) {
        long now = clock.instant().getEpochSecond() / TIME_STEP_SECONDS;
        return computeCode(secretBase32, now);
    }

    /**
     * Verifica código aceitando ±1 janela (clock skew tolerável de 30s
     * pra cada lado, conforme RFC 6238 recomendação).
     */
    public boolean verifyCode(String secretBase32, String code) {
        if (code == null || code.length() != CODE_DIGITS) return false;
        long now = clock.instant().getEpochSecond() / TIME_STEP_SECONDS;
        for (int delta = -1; delta <= 1; delta++) {
            if (computeCode(secretBase32, now + delta).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String computeCode(String secretBase32, long counter) {
        try {
            byte[] secret = base32Decode(secretBase32);
            ByteBuffer buf = ByteBuffer.allocate(8).putLong(counter);
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = hmac.doFinal(buf.array());
            int offset = hash[hash.length - 1] & 0x0F;
            int truncated = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = truncated % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao calcular TOTP", ex);
        }
    }

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int idx = (buffer >> (bitsLeft - 5)) & 0x1F;
                sb.append(BASE32_ALPHABET[idx]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int idx = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_ALPHABET[idx]);
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String s) {
        s = s.toUpperCase().replace("=", "");
        int buffer = 0;
        int bitsLeft = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : s.toCharArray()) {
            int v = -1;
            for (int i = 0; i < BASE32_ALPHABET.length; i++) {
                if (BASE32_ALPHABET[i] == c) { v = i; break; }
            }
            if (v < 0) throw new IllegalArgumentException("char inválido base32: " + c);
            buffer = (buffer << 5) | v;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
