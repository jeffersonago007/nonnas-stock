package com.nonnas.identity.application.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento auditável não-CRUD. CRUD em entidades de domínio fica para
 * Hibernate Envers (T17/T18). Esta tabela cobre eventos sistêmicos que
 * precisam aparecer em uma trilha consultável: login, logout, alteração
 * de perfil, bloqueio por brute force, exercício de direitos LGPD.
 *
 * <p>Por simplicidade, {@code metadata} é JSON plano em string; o adapter
 * persiste como {@code jsonb}. Para schemas mais ricos, evoluir depois.
 */
public record AuditEvent(
        Instant occurredAt,
        UUID actorId,        // null = sistema (scheduler, etc.)
        String actorEmail,
        String eventType,
        String targetKind,
        UUID targetId,
        String requestIp,
        String requestUserAgent,
        String metadataJson  // ex.: {"motivo":"5 tentativas falhas"}
) {

    public static final class Types {
        public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String LOGIN_BLOQUEADO = "LOGIN_BLOQUEADO";
        public static final String LOGOUT = "LOGOUT";
        public static final String SENHA_TROCADA = "SENHA_TROCADA";
        public static final String PERFIL_ALTERADO = "PERFIL_ALTERADO";
        public static final String TWO_FA_HABILITADO = "TWO_FA_HABILITADO";
        public static final String TWO_FA_REMOVIDO = "TWO_FA_REMOVIDO";
        public static final String LGPD_DADOS_SOLICITADOS = "LGPD_DADOS_SOLICITADOS";
        public static final String LGPD_CORRECAO = "LGPD_CORRECAO";
        public static final String LGPD_EXCLUSAO = "LGPD_EXCLUSAO";

        private Types() {}
    }
}
