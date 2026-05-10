package com.nonnas.nfeimporter.domain;

/**
 * Dados do emitente extraídos do XML NF-e (tag {@code <emit>}). É a fonte
 * canônica para localizar/criar Fornecedor durante o processamento.
 */
public record EmitenteLido(
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String inscricaoEstadual
) {}
