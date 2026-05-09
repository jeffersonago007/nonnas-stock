package com.nonnas.recipes.application.ports;

import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.FichaTecnicaId;
import com.nonnas.recipes.domain.ProdutoVendavelId;

import java.util.List;
import java.util.Optional;

public interface FichaTecnicaRepository {
    /**
     * Persiste a ficha — INSERT se nova, UPDATE se existente. O adapter
     * faz {@code saveAndFlush} para garantir ordem de SQL no fluxo de
     * versionamento (UPDATE da antiga antes do INSERT da nova evita
     * violação do partial unique index).
     */
    FichaTecnica save(FichaTecnica f);

    Optional<FichaTecnica> findById(FichaTecnicaId id);

    /** Ficha ativa do produto, se existir. */
    Optional<FichaTecnica> findVigentePorProduto(ProdutoVendavelId produtoId);

    /** Histórico completo do produto, da versão mais recente para a mais antiga. */
    List<FichaTecnica> findHistoricoPorProduto(ProdutoVendavelId produtoId);
}
