package com.nonnas.identity.application.auth;

import com.nonnas.identity.application.audit.AuditEvent;
import com.nonnas.identity.application.audit.AuditLogService;
import com.nonnas.identity.application.ports.TokensRevogadosPort;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import com.nonnas.identity.infrastructure.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Encerra a sessão atual: adiciona o JTI do access token ao blacklist e
 * revoga toda a família de refresh tokens do usuário (master doc T16:
 * "Token blacklist em logout").
 */
@Service
public class LogoutUseCase {

    private final JwtTokenProvider tokenProvider;
    private final TokensRevogadosPort blacklist;
    private final UsuarioRepository usuarioRepo;
    private final AuditLogService auditLog;
    private final com.nonnas.identity.infrastructure.security.RefreshTokenService refreshTokens;
    private final Clock clock;

    public LogoutUseCase(JwtTokenProvider tokenProvider,
                         TokensRevogadosPort blacklist,
                         UsuarioRepository usuarioRepo,
                         AuditLogService auditLog,
                         com.nonnas.identity.infrastructure.security.RefreshTokenService refreshTokens,
                         Clock clock) {
        this.tokenProvider = tokenProvider;
        this.blacklist = blacklist;
        this.usuarioRepo = usuarioRepo;
        this.auditLog = auditLog;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Transactional
    public void execute(String accessToken, String refreshToken) {
        JwtTokenProvider.ParsedAccess parsed = tokenProvider.parseAccess(accessToken);
        blacklist.revogar(
                parsed.jti().toString(),
                parsed.usuarioId().value(),
                parsed.expiresAt(),
                TokensRevogadosPort.Motivo.LOGOUT);

        // Revogação da família de refresh tokens — quando informado, derruba
        // toda a árvore. Best-effort: logout sem refresh ainda invalida o
        // access token, que era o objetivo principal.
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                JwtTokenProvider.ParsedRefresh parsedRefresh = tokenProvider.parseRefresh(refreshToken);
                refreshTokens.revogarFamilia(parsedRefresh.familyId(), clock.instant());
            } catch (RuntimeException ignored) {
                // Refresh inválido ou expirado: nada a revogar; o access já
                // está blacklisted.
            }
        }

        Usuario usuario = usuarioRepo.findById(UsuarioId.of(parsed.usuarioId().value()))
                .orElse(null);
        auditLog.registrarTentativaLogin(AuditEvent.Types.LOGOUT, usuario, null, null, null);
    }
}
