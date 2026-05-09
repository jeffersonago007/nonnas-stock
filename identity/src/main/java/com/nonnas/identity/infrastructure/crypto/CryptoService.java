package com.nonnas.identity.infrastructure.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Serviço de criptografia AES-GCM autenticado para "campos sensíveis"
 * (master doc 13.2). Apenas Java/JCE — sem BouncyCastle.
 *
 * <p>Formato persistido: base64( iv (12 bytes) || ciphertext || tag (16 bytes) ).
 * O tag GCM detecta adulteração; alteração silenciosa do ciphertext gera
 * exceção no decrypt.
 *
 * <p>Em prod, chave 32 bytes em base64 vem de {@code NONNAS_MASTER_KEY}.
 * Em dev/test, derivamos uma chave determinística do profile só pra não
 * abortar o boot — secrets nesses ambientes não devem ser sensíveis.
 */
@Service
@EnableConfigurationProperties(NonnasCryptoProperties.class)
public class CryptoService {

    private static final int IV_LEN = 12;
    private static final int TAG_LEN_BITS = 128;
    // base64 de "0123456789012345678901234567890a" — exatamente 32 bytes (AES-256).
    // Apenas para dev/test; em prod, NONNAS_MASTER_KEY é obrigatório.
    private static final String DEV_FALLBACK_BASE64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MGE=";

    private final NonnasCryptoProperties props;
    private final Environment environment;
    private final SecureRandom rng = new SecureRandom();
    private SecretKey key;

    public CryptoService(NonnasCryptoProperties props, Environment environment) {
        this.props = props;
        this.environment = environment;
    }

    @PostConstruct
    void init() {
        String configured = props.masterKey();
        boolean isProd = false;
        for (String p : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(p)) {
                isProd = true;
                break;
            }
        }
        if ((configured == null || configured.isBlank()) && isProd) {
            throw new IllegalStateException(
                    "NONNAS_MASTER_KEY ausente em profile prod. Configure 32 bytes em base64.");
        }
        String effective = (configured != null && !configured.isBlank()) ? configured : DEV_FALLBACK_BASE64;
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(effective);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("NONNAS_MASTER_KEY não é base64 válido", ex);
        }
        if (raw.length != 32) {
            throw new IllegalStateException(
                    "NONNAS_MASTER_KEY deve ter 32 bytes (AES-256); recebido " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            rng.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv);
            buf.put(ct);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao criptografar", ex);
        }
    }

    public String decrypt(String cipherBase64) {
        if (cipherBase64 == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(cipherBase64);
            if (all.length < IV_LEN + 16) {
                throw new IllegalStateException("ciphertext truncado");
            }
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[all.length - IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            System.arraycopy(all, IV_LEN, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao descriptografar — ciphertext adulterado ou chave incorreta", ex);
        }
    }
}
