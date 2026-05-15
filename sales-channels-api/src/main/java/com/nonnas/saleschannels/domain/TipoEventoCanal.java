package com.nonnas.saleschannels.domain;

/**
 * Tipos de evento canônicos recebidos do canal — mapeamento direto do
 * ciclo Open Delivery (PLC/CFM/DSP/CON/CAN). Cada adapter de canal
 * traduz seus eventos nativos para este enum.
 */
public enum TipoEventoCanal {
    PEDIDO_CRIADO,
    PEDIDO_CONFIRMADO,
    PEDIDO_DESPACHADO,
    PEDIDO_CONCLUIDO,
    PEDIDO_CANCELADO,
    OUTRO
}
