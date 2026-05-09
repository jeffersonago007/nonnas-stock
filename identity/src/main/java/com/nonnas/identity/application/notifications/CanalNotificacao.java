package com.nonnas.identity.application.notifications;

/**
 * Ponto de extensão para canais futuros (email, WhatsApp). Hoje a única
 * implementação é {@code CanalInterno} (grava na tabela
 * {@code notificacoes_usuario}). Master doc 15.4 deixa o slot.
 */
public interface CanalNotificacao {

    /** Identificador do canal — bate com a coluna {@code canais_destino}. */
    String nome();

    /** Despacha a notificação para este canal. Idempotente. */
    void despachar(NovaNotificacao notificacao);
}
