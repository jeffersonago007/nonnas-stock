package com.nonnas.saleschannels.infrastructure.crypto;

import com.nonnas.saleschannels.application.ports.SegredoCifrador;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Implementação default da {@link SegredoCifrador} — Base64. Este é um
 * <strong>placeholder</strong>: NÃO é cifragem real, apenas evita que o
 * segredo apareça em texto puro em logs/grep casual. A produção deve
 * sobrescrever este bean com uma impl AES-256-GCM via {@code @Primary}
 * no {@code app/}, usando o {@code CryptoService} de T16.
 *
 * <p>O prefixo {@code "B64:"} torna evidente em qualquer ferramenta de
 * inspeção que esta credencial NÃO está cifrada de verdade — alerta
 * operacional intencional.
 */
@Component
class SegredoCifradorBase64 implements SegredoCifrador {

    private static final String PREFIXO = "B64:";

    @Override
    public String cifrar(String emClaro) {
        if (emClaro == null) return null;
        return PREFIXO + Base64.getEncoder().encodeToString(emClaro.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decifrar(String cifrado) {
        if (cifrado == null) return null;
        String semPrefixo = cifrado.startsWith(PREFIXO) ? cifrado.substring(PREFIXO.length()) : cifrado;
        return new String(Base64.getDecoder().decode(semPrefixo), StandardCharsets.UTF_8);
    }
}
