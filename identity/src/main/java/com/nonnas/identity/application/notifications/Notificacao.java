package com.nonnas.identity.application.notifications;

import java.time.Instant;
import java.util.UUID;

/**
 * Snapshot de uma notificação pra UI/clientes. Não confundir com a entidade
 * JPA ({@code NotificacaoEntity}) ou com o evento de domínio que dispara
 * a criação.
 */
public record Notificacao(
        UUID id,
        UUID usuarioId,
        String tipo,
        Prioridade prioridade,
        String titulo,
        String mensagem,
        String linkAcao,
        String metadataJson,
        String canaisDestino,
        Instant criadaEm,
        Instant lidaEm,
        Instant arquivadaEm
) {

    public enum Prioridade { INFO, AVISO, CRITICA }

    /**
     * Tipos pré-definidos. Adicionar aqui ao introduzir novo evento de
     * domínio que crie notificação — ajuda o frontend a renderizar ícone
     * + cor consistentes.
     */
    public static final class Tipos {
        public static final String ALERTA_DISPARADO = "ALERTA_DISPARADO";
        public static final String TRANSFERENCIA_APROVADA = "TRANSFERENCIA_APROVADA";
        public static final String TRANSFERENCIA_RECEBIDA = "TRANSFERENCIA_RECEBIDA";
        public static final String DIVERGENCIA_INVENTARIO = "DIVERGENCIA_INVENTARIO";
        public static final String LOGIN_NOVO_IP = "LOGIN_NOVO_IP";
        public static final String LGPD_DIREITO_EXERCIDO = "LGPD_DIREITO_EXERCIDO";

        private Tipos() {}
    }
}
