package com.nonnas.inventory.infrastructure.persistence;

import com.nonnas.inventory.domain.ItemMovimentacao;
import com.nonnas.inventory.domain.Lote;
import com.nonnas.inventory.domain.LoteId;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.MovimentacaoId;
import com.nonnas.inventory.domain.SaldoLote;

import java.util.List;

final class InventoryMappers {
    private InventoryMappers() {}

    static LoteEntity toEntity(Lote l) {
        LoteEntity e = new LoteEntity();
        e.setId(l.id().value());
        e.setInsumoId(l.insumoId());
        e.setFornecedorId(l.fornecedorId());
        e.setNotaFiscalId(l.notaFiscalId());
        e.setNumeroLote(l.numeroLote());
        e.setDataFabricacao(l.dataFabricacao());
        e.setDataValidade(l.dataValidade());
        e.setValorUnitario(l.valorUnitario());
        e.setCreatedAt(l.createdAt());
        return e;
    }

    static Lote toDomain(LoteEntity e) {
        return new Lote(LoteId.of(e.getId()), e.getInsumoId(), e.getFornecedorId(),
                e.getNotaFiscalId(), e.getNumeroLote(), e.getDataFabricacao(), e.getDataValidade(),
                e.getValorUnitario(), e.getCreatedAt());
    }

    static SaldoLoteEntity toEntity(SaldoLote s) {
        SaldoLoteEntity e = new SaldoLoteEntity();
        e.setLoteId(s.loteId().value());
        e.setFilialId(s.filialId());
        e.setQuantidadeBase(s.quantidadeBase());
        e.setAtualizadoEm(s.atualizadoEm());
        return e;
    }

    static SaldoLote toDomain(SaldoLoteEntity e) {
        return new SaldoLote(LoteId.of(e.getLoteId()), e.getFilialId(),
                e.getQuantidadeBase(), e.getAtualizadoEm());
    }

    static MovimentacaoEntity toEntity(Movimentacao m) {
        MovimentacaoEntity e = new MovimentacaoEntity();
        e.setId(m.id().value());
        e.setFilialId(m.filialId());
        e.setUsuarioId(m.usuarioId());
        e.setTipo(m.tipo());
        e.setDataMovimentacao(m.dataMovimentacao());
        e.setDocumentoOrigemTipo(m.documentoOrigemTipo());
        e.setDocumentoOrigemId(m.documentoOrigemId());
        e.setObservacao(m.observacao());
        e.setGerouNegativo(m.gerouNegativo());
        e.setCreatedAt(m.createdAt());
        return e;
    }

    static Movimentacao toDomain(MovimentacaoEntity e, List<ItemMovimentacaoEntity> items) {
        List<ItemMovimentacao> domainItems = items.stream().map(InventoryMappers::toDomain).toList();
        return new Movimentacao(
                MovimentacaoId.of(e.getId()),
                e.getFilialId(), e.getUsuarioId(), e.getTipo(), e.getDataMovimentacao(),
                e.getDocumentoOrigemTipo(), e.getDocumentoOrigemId(), e.getObservacao(),
                e.isGerouNegativo(), domainItems, e.getCreatedAt());
    }

    static ItemMovimentacaoEntity toEntity(ItemMovimentacao i, java.util.UUID movimentacaoId) {
        ItemMovimentacaoEntity e = new ItemMovimentacaoEntity();
        e.setId(i.id());
        e.setMovimentacaoId(movimentacaoId);
        e.setInsumoId(i.insumoId());
        e.setLoteId(i.loteId().value());
        e.setUnidadeLancamentoId(i.unidadeLancamentoId());
        e.setQuantidadeLancada(i.quantidadeLancada());
        e.setQuantidadeBase(i.quantidadeBase());
        e.setValorUnitario(i.valorUnitario());
        return e;
    }

    static ItemMovimentacao toDomain(ItemMovimentacaoEntity e) {
        return new ItemMovimentacao(e.getId(), e.getInsumoId(), LoteId.of(e.getLoteId()),
                e.getUnidadeLancamentoId(), e.getQuantidadeLancada(), e.getQuantidadeBase(),
                e.getValorUnitario());
    }
}
