package com.nonnas.nfeimporter.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Representação intermediária de uma NF-e modelo 55 já parseada. Não tem
 * persistência associada — é apenas o resultado do parser, consumido pelo
 * use case que decide criar/localizar Fornecedor + Insumos e disparar a
 * movimentação de entrada.
 *
 * <p>Distinto de {@code com.nonnas.operations.domain.NotaFiscal}, que é
 * a entidade persistente do bounded context de operações.
 */
public record NotaFiscalLida(
        String chaveAcesso,
        String numero,
        String serie,
        OffsetDateTime dataEmissao,
        BigDecimal valorTotal,
        EmitenteLido emitente,
        List<ItemLido> itens
) {}
