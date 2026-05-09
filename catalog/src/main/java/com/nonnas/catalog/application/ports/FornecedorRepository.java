package com.nonnas.catalog.application.ports;

import com.nonnas.catalog.domain.Cnpj;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.catalog.domain.FornecedorId;

import java.util.List;
import java.util.Optional;

public interface FornecedorRepository {
    Fornecedor save(Fornecedor f);
    Optional<Fornecedor> findById(FornecedorId id);
    Optional<Fornecedor> findByCnpj(Cnpj cnpj);
    boolean existsByCnpj(Cnpj cnpj);
    List<Fornecedor> findAll(int page, int size);

    /**
     * @param ativo quando não-nulo filtra pelo status ativo/inativo.
     * @param q     quando não-nulo/não-vazio aplica busca case-insensitive em razão social ou CNPJ.
     */
    List<Fornecedor> findFiltered(Boolean ativo, String q, int page, int size);
}
