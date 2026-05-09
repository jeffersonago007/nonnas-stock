package com.nonnas.sharedkernel.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de domínio publicado quando um alerta novo é registrado em
 * {@code alertas_disparados}. Listeners (notificações internas, métricas
 * Prometheus) consomem sem acoplar com o módulo alerts.
 *
 * <p>Vive em shared-kernel pra não criar dependência módulo-a-módulo só
 * por causa de uma DTO de evento.
 */
public record AlertaDisparadoEvent(
        UUID disparadoId,
        UUID configId,
        String tipo,             // RUPTURA, ESTOQUE_MINIMO_*, VENCIMENTO_PROXIMO_DIAS
        UUID insumoId,
        UUID filialId,
        UUID loteId,             // null para alertas não-lote
        BigDecimal saldoNoDisparo,
        String prioridade,       // BAIXA, MEDIA, ALTA, CRITICA
        Instant disparadoEm
) {
}
