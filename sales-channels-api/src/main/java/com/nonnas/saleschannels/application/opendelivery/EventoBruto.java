package com.nonnas.saleschannels.application.opendelivery;

import com.nonnas.saleschannels.domain.TipoEventoCanal;

import java.time.Instant;

/**
 * Evento bruto retornado pelo polling do canal — antes de virar
 * {@link com.nonnas.saleschannels.domain.EventoCanal}. Contém o payload
 * cru (JSON) e os campos canônicos suficientes para idempotência
 * ({@code eventIdExterno}) e roteamento ({@code tipoEvento}).
 */
public record EventoBruto(
        String eventIdExterno,
        TipoEventoCanal tipoEvento,
        String pedidoExternoId,
        String merchantExternoId,
        Instant ocorridoEm,
        String payloadJson
) {}
