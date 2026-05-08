package com.nonnas.catalog.domain;

/**
 * Tipo de grandeza de uma unidade de medida. Conversões globais só fazem
 * sentido entre unidades do mesmo tipo (ex.: KG → G é PESO → PESO).
 * Conversões específicas por insumo (ex.: CX de mussarela → KG, fator 5)
 * podem cruzar tipos porque carregam contexto do insumo.
 */
public enum UnidadeMedidaTipo {
    PESO,
    VOLUME,
    UNIDADE
}
