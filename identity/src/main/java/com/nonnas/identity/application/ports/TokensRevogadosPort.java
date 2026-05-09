package com.nonnas.identity.application.ports;

import java.time.Instant;
import java.util.UUID;

/**
 * Blacklist de access tokens JWT. Logout grava o JTI; o filter de
 * autenticação consulta antes de aceitar a request.
 *
 * <p>Auto-purga: implementação pode varrer registros expirados
 * periodicamente; por enquanto a tabela só cresce até a expiração natural
 * do token (TTL de 60min default).
 */
public interface TokensRevogadosPort {

    enum Motivo { LOGOUT, TROCA_SENHA, ADMIN_FORCE, TWO_FA_HABILITADO }

    void revogar(String jti, UUID usuarioId, Instant expiraEm, Motivo motivo);

    boolean estaRevogado(String jti);
}
