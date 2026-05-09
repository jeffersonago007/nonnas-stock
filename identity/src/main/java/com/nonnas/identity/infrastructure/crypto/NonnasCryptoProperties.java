package com.nonnas.identity.infrastructure.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chave mestra de criptografia em base64 (32 bytes = AES-256).
 * Variável de ambiente: {@code NONNAS_MASTER_KEY} (mapeada via Spring relaxed binding).
 *
 * <p>Em dev/teste, default não-seguro é tolerado (ver
 * {@link CryptoService#init()}). Em produção, ausência da variável aborta o
 * boot — comportamento explícito no master doc 13.2 (chave mestra obrigatória).
 */
@ConfigurationProperties(prefix = "nonnas.crypto")
public record NonnasCryptoProperties(String masterKey) {
}
