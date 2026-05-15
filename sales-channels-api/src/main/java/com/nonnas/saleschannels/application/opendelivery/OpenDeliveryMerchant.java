package com.nonnas.saleschannels.application.opendelivery;

/**
 * Loja no canal — Open Delivery v1.0.1.
 *
 * <p>{@code id} é o identificador do canal (ex.: {@code merchantId} iFood).
 * O lookup para {@code filial_id} Nonnas é feito via tabela
 * {@code canais_credenciais} (T-CANAL-01).
 */
public record OpenDeliveryMerchant(String id, String name) {}
