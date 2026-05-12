package com.nonnas.operations.application.ports;

import com.nonnas.operations.domain.FornecedorInsumoDePara;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FornecedorInsumoDeParaRepository {

    FornecedorInsumoDePara save(FornecedorInsumoDePara depara);

    /**
     * Lookup canônico usado pelo preview-xml para sugerir match automático
     * de item da nota com insumo já mapeado deste fornecedor.
     */
    Optional<FornecedorInsumoDePara> findByFornecedorAndCodigo(UUID fornecedorId, String codigoFornecedor);

    /** Todos os mapeamentos de um fornecedor (uso administrativo / debug). */
    List<FornecedorInsumoDePara> findByFornecedor(UUID fornecedorId);

    /** Remove o mapeamento (fornecedor, código) — usado quando o operador identifica
     *  um vínculo aprendido errado e quer destravar reuso indevido. */
    void deleteByFornecedorAndCodigo(UUID fornecedorId, String codigoFornecedor);
}
