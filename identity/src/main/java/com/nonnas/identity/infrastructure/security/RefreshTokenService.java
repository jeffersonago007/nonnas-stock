package com.nonnas.identity.infrastructure.security;

import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.infrastructure.persistence.RefreshTokenEntity;
import com.nonnas.identity.infrastructure.persistence.RefreshTokenJpaRepository;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token rotation com detecção de replay (ADR 0003).
 *
 * <p>Cada uso de um refresh token o invalida (rotação) e emite um novo,
 * preservando o {@code familyId} original. Se um refresh já invalidado
 * é apresentado de novo, a família inteira é revogada — sinal forte de
 * que o token foi roubado.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenJpaRepository repository;
    private final JwtTokenProvider tokenProvider;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenJpaRepository repository,
                               JwtTokenProvider tokenProvider,
                               Clock clock) {
        this.repository = repository;
        this.tokenProvider = tokenProvider;
        this.clock = clock;
    }

    /**
     * Issues a brand-new family for a fresh login. No parent.
     */
    @Transactional
    public JwtTokenProvider.IssuedToken issueNewFamily(Usuario usuario) {
        UUID familyId = UUID.randomUUID();
        return persistAndReturn(usuario, familyId, null);
    }

    /**
     * Rotates a refresh token: validates, marks as revoked, emits a new one
     * in the same family with parentJti pointing at the consumed token.
     *
     * <p>{@code noRollbackFor = BusinessRuleException} é crítico porque, no
     * caminho de replay, precisamos que o {@code revokeFamily} persista mesmo
     * quando lançamos a exceção. Sem essa cláusula, o rollback default desfaz
     * a revogação e a família continua válida.
     *
     * @throws BusinessRuleException with code UNAUTHORIZED on replay or expiration
     */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public JwtTokenProvider.IssuedToken rotate(Usuario usuario, JwtTokenProvider.ParsedRefresh parsed) {
        Instant now = clock.instant();

        RefreshTokenEntity entity = repository.findByJti(parsed.jti())
                .orElseThrow(() -> new BusinessRuleException(
                        ErrorCode.UNAUTHORIZED, "Refresh token desconhecido"));

        if (entity.getRevokedAt() != null) {
            // Replay! revoke the entire family.
            repository.revokeFamily(entity.getFamilyId(), now);
            throw new BusinessRuleException(
                    ErrorCode.UNAUTHORIZED, "Refresh token reutilizado — sessão revogada");
        }

        if (entity.getExpiresAt().isBefore(now)) {
            throw new BusinessRuleException(
                    ErrorCode.UNAUTHORIZED, "Refresh token expirado");
        }

        // Mark as revoked
        entity.setRevokedAt(now);
        repository.save(entity);

        return persistAndReturn(usuario, entity.getFamilyId(), entity.getJti());
    }

    @Transactional
    public void revokeAllForUser(UUID usuarioId) {
        // simplest: full scan revoke would be a query; for MVP we don't expose this yet.
        // Placeholder for password-change-driven revocation (T16).
    }

    /**
     * Revoga todos os tokens de uma família (logout, troca de senha, ou
     * habilitação de 2FA). Idempotente — chamadas repetidas não recriam
     * efeito, apenas marcam novamente {@code revoked_at} (no-op no SQL
     * porque a query já filtra {@code revoked_at IS NULL}).
     */
    @Transactional
    public void revogarFamilia(UUID familyId, Instant agora) {
        repository.revokeFamily(familyId, agora);
    }

    private JwtTokenProvider.IssuedToken persistAndReturn(Usuario usuario, UUID familyId, UUID parentJti) {
        JwtTokenProvider.IssuedToken issued = tokenProvider.issueRefresh(usuario, familyId, parentJti);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setJti(issued.jti());
        entity.setFamilyId(familyId);
        entity.setParentJti(parentJti);
        entity.setUsuarioId(usuario.id().value());
        entity.setExpiresAt(issued.expiresAt());
        entity.setRevokedAt(null);
        entity.setCreatedAt(clock.instant());
        repository.save(entity);

        return issued;
    }
}
