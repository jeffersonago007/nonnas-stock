package com.nonnas.identity.interfaces.rest.dto;

import com.nonnas.identity.application.notifications.Notificacao;

import java.time.Instant;
import java.util.UUID;

public final class NotificacaoDto {

    public record Response(
            UUID id,
            String tipo,
            String prioridade,
            String titulo,
            String mensagem,
            String linkAcao,
            String metadata,
            Instant criadaEm,
            Instant lidaEm,
            Instant arquivadaEm
    ) {
        public static Response from(Notificacao n) {
            return new Response(n.id(), n.tipo(), n.prioridade().name(),
                    n.titulo(), n.mensagem(), n.linkAcao(), n.metadataJson(),
                    n.criadaEm(), n.lidaEm(), n.arquivadaEm());
        }
    }

    public record ContagemResponse(long naoLidas) {}

    private NotificacaoDto() {}
}
