package com.nonnas.identity.application.audit;

import com.nonnas.identity.application.ports.AuditLogPort;
import com.nonnas.identity.domain.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Wrapper de conveniência sobre {@link AuditLogPort}. Centraliza factories
 * para os tipos de evento mais comuns e injeta o relógio + dados do request
 * (em chamadores que tiverem acesso ao {@link jakarta.servlet.http.HttpServletRequest}).
 *
 * <p>{@code @Transactional(propagation = REQUIRES_NEW)} garante que a
 * gravação do log <em>persiste</em> mesmo se a transação chamadora der
 * rollback (ex.: {@code AutenticarUseCase} faz rollback ao falhar mas
 * queremos a entrada {@code LOGIN_FAILED} no log).
 */
@Service
public class AuditLogService {

    private final AuditLogPort port;
    private final Clock clock;

    public AuditLogService(AuditLogPort port, Clock clock) {
        this.port = port;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) {
        port.record(event);
    }

    /** Conveniência para LOGIN_SUCCESS / LOGIN_FAILED / LOGIN_BLOQUEADO. */
    public void registrarTentativaLogin(String tipo, Usuario usuario, String ip, String userAgent,
                                        String metadataJson) {
        UUID actorId = usuario != null ? usuario.id().value() : null;
        String email = usuario != null ? usuario.email().value() : null;
        record(new AuditEvent(
                clock.instant(),
                actorId,
                email,
                tipo,
                usuario != null ? "USUARIO" : null,
                actorId,
                ip,
                userAgent,
                metadataJson));
    }
}
