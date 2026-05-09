package com.nonnas.operations.application.ports;

import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.domain.AjusteEstoqueId;
import com.nonnas.operations.domain.StatusAjuste;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AjusteEstoqueRepository {
    AjusteEstoque save(AjusteEstoque a);
    Optional<AjusteEstoque> findById(AjusteEstoqueId id);
    List<AjusteEstoque> findByFilialEStatus(UUID filialId, StatusAjuste status, int page, int size);
    List<AjusteEstoque> findPendentes(int page, int size);
}
