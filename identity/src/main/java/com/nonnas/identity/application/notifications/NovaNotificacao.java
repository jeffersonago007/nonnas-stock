package com.nonnas.identity.application.notifications;

import java.util.UUID;

/**
 * Comando para criar uma notificação. Recebido pelos {@code @EventListener}
 * que escutam eventos de domínio (alerta disparado, transferência mudou de
 * status, etc.) e o convertem em notificação para o usuário-alvo.
 */
public record NovaNotificacao(
        UUID usuarioId,
        String tipo,
        Notificacao.Prioridade prioridade,
        String titulo,
        String mensagem,
        String linkAcao,
        String metadataJson,
        String canaisDestino    // CSV — default "INTERNO"
) {

    public static NovaNotificacao interna(UUID usuarioId, String tipo,
                                          Notificacao.Prioridade prioridade,
                                          String titulo, String mensagem,
                                          String linkAcao, String metadataJson) {
        return new NovaNotificacao(usuarioId, tipo, prioridade, titulo, mensagem,
                linkAcao, metadataJson, "INTERNO");
    }
}
