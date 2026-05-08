package com.nonnas.inventory.application.ports;

import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.MovimentacaoId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimentacaoRepository {
    Movimentacao save(Movimentacao m);
    Optional<Movimentacao> findById(MovimentacaoId id);
    List<Movimentacao> findByFilial(UUID filialId, int page, int size);
}
