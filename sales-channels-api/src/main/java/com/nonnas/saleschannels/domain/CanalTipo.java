package com.nonnas.saleschannels.domain;

/**
 * Canais de venda suportados. Todos comunicam-se via padrão Open Delivery
 * (Abrasel) — ver ADR 0016. O genérico {@link #OPEN_DELIVERY_GENERICO}
 * existe para o caminho de mock/desenvolvimento e para qualquer canal
 * futuro que adira ao padrão.
 */
public enum CanalTipo {
    IFOOD,
    NOVENTANOVE_FOOD,
    KEETA,
    OPEN_DELIVERY_GENERICO
}
