package com.nonnas.saleschannels.application.opendelivery;

/**
 * Cliente do pedido — Open Delivery v1.0.1 (subset).
 *
 * <p>Campos identificadores (documentNumber, email) ficam fora do POC.
 * São dados pessoais que disparam obrigações LGPD adicionais; quando o
 * POC virar produto, adicionam-se com fluxos de retenção/exclusão
 * próprios (T16 já tem a infra).
 */
public record OpenDeliveryCustomer(String id, String name, String phone) {}
