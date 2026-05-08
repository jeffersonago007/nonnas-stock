package com.nonnas.inventory.infrastructure.persistence;

import com.nonnas.inventory.application.ports.MovimentacaoRepository;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.MovimentacaoId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class MovimentacaoRepositoryImpl implements MovimentacaoRepository {

    private final SpringDataMovimentacaoRepository movJpa;
    private final SpringDataItemMovimentacaoRepository itemJpa;

    MovimentacaoRepositoryImpl(SpringDataMovimentacaoRepository movJpa,
                               SpringDataItemMovimentacaoRepository itemJpa) {
        this.movJpa = movJpa;
        this.itemJpa = itemJpa;
    }

    @Override public Movimentacao save(Movimentacao m) {
        MovimentacaoEntity me = InventoryMappers.toEntity(m);
        movJpa.save(me);
        UUID movId = me.getId();
        for (var item : m.itens()) {
            itemJpa.save(InventoryMappers.toEntity(item, movId));
        }
        return m;
    }

    @Override public Optional<Movimentacao> findById(MovimentacaoId id) {
        return movJpa.findById(id.value())
                .map(me -> InventoryMappers.toDomain(me, itemJpa.findByMovimentacaoId(me.getId())));
    }

    @Override public List<Movimentacao> findByFilial(UUID filialId, int page, int size) {
        return movJpa.findByFilialIdOrderByDataMovimentacaoDesc(filialId, PageRequest.of(page, size))
                .map(me -> InventoryMappers.toDomain(me, itemJpa.findByMovimentacaoId(me.getId())))
                .getContent();
    }
}
