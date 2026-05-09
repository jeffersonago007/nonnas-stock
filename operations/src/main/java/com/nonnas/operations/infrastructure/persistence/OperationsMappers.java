package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.domain.AjusteEstoqueId;
import com.nonnas.operations.domain.CargaInicial;
import com.nonnas.operations.domain.CargaInicialId;
import com.nonnas.operations.domain.ItemTransferencia;
import com.nonnas.operations.domain.Transferencia;
import com.nonnas.operations.domain.TransferenciaId;

import java.util.List;

final class OperationsMappers {

    private OperationsMappers() {}

    // ItemTransferencia
    static ItemTransferenciaEntity toEntity(ItemTransferencia i) {
        ItemTransferenciaEntity e = new ItemTransferenciaEntity();
        e.setId(i.id());
        e.setInsumoId(i.insumoId());
        e.setUnidadeId(i.unidadeId());
        e.setQuantidadeSolicitada(i.quantidadeSolicitada());
        e.setQuantidadeRecebida(i.quantidadeRecebidaOpt().orElse(null));
        return e;
    }

    static ItemTransferencia toDomain(ItemTransferenciaEntity e) {
        return new ItemTransferencia(e.getId(), e.getInsumoId(), e.getUnidadeId(),
                e.getQuantidadeSolicitada(), e.getQuantidadeRecebida());
    }

    // Transferencia
    static TransferenciaEntity toEntity(Transferencia t) {
        TransferenciaEntity e = new TransferenciaEntity();
        e.setId(t.id().value());
        e.setFilialOrigemId(t.filialOrigemId());
        e.setFilialDestinoId(t.filialDestinoId());
        e.setStatus(t.status());
        e.setSolicitadoPor(t.solicitadoPor());
        e.setAprovadoPor(t.aprovadoPorOpt().orElse(null));
        e.setEnviadoPor(t.enviadoPorOpt().orElse(null));
        e.setRecebidoPor(t.recebidoPorOpt().orElse(null));
        e.setDataSolicitacao(t.dataSolicitacao());
        e.setDataAprovacao(t.dataAprovacaoOpt().orElse(null));
        e.setDataEnvio(t.dataEnvioOpt().orElse(null));
        e.setDataRecebimento(t.dataRecebimentoOpt().orElse(null));
        e.setObservacao(t.observacaoOpt().orElse(null));
        e.setMovSaidaId(t.movSaidaIdOpt().orElse(null));
        e.setMovEntradaId(t.movEntradaIdOpt().orElse(null));
        e.setMotivoCancelamento(t.motivoCancelamentoOpt().orElse(null));
        e.setCreatedAt(t.createdAt());
        e.setUpdatedAt(t.updatedAt());
        e.getItens().clear();
        for (var item : t.itens()) {
            e.getItens().add(toEntity(item));
        }
        return e;
    }

    static Transferencia toDomain(TransferenciaEntity e) {
        List<ItemTransferencia> itens = e.getItens().stream()
                .map(OperationsMappers::toDomain)
                .toList();
        return new Transferencia(
                TransferenciaId.of(e.getId()),
                e.getFilialOrigemId(), e.getFilialDestinoId(),
                e.getStatus(),
                e.getSolicitadoPor(), e.getAprovadoPor(), e.getEnviadoPor(), e.getRecebidoPor(),
                e.getDataSolicitacao(), e.getDataAprovacao(), e.getDataEnvio(), e.getDataRecebimento(),
                e.getObservacao(), e.getMovSaidaId(), e.getMovEntradaId(), e.getMotivoCancelamento(),
                itens, e.getCreatedAt(), e.getUpdatedAt());
    }

    // AjusteEstoque
    static AjusteEstoqueEntity toEntity(AjusteEstoque a) {
        AjusteEstoqueEntity e = new AjusteEstoqueEntity();
        e.setId(a.id().value());
        e.setFilialId(a.filialId());
        e.setInsumoId(a.insumoId());
        e.setUnidadeId(a.unidadeId());
        e.setQuantidadeDiff(a.quantidadeDiff());
        e.setMotivo(a.motivo());
        e.setStatus(a.status());
        e.setRequerAprovacao(a.requerAprovacao());
        e.setSolicitadoPor(a.solicitadoPor());
        e.setAprovadoPor(a.aprovadoPorOpt().orElse(null));
        e.setDataSolicitacao(a.dataSolicitacao());
        e.setDataAprovacao(a.dataAprovacaoOpt().orElse(null));
        e.setMovId(a.movIdOpt().orElse(null));
        e.setOrigemTransferenciaId(a.origemTransferenciaIdOpt().orElse(null));
        e.setRejeicaoMotivo(a.rejeicaoMotivoOpt().orElse(null));
        e.setCreatedAt(a.createdAt());
        e.setUpdatedAt(a.updatedAt());
        return e;
    }

    static AjusteEstoque toDomain(AjusteEstoqueEntity e) {
        return new AjusteEstoque(
                AjusteEstoqueId.of(e.getId()),
                e.getFilialId(), e.getInsumoId(), e.getUnidadeId(),
                e.getQuantidadeDiff(), e.getMotivo(), e.getStatus(),
                e.isRequerAprovacao(), e.getSolicitadoPor(), e.getAprovadoPor(),
                e.getDataSolicitacao(), e.getDataAprovacao(),
                e.getMovId(), e.getOrigemTransferenciaId(), e.getRejeicaoMotivo(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    // CargaInicial
    static CargaInicialEntity toEntity(CargaInicial c) {
        CargaInicialEntity e = new CargaInicialEntity();
        e.setId(c.id().value());
        e.setFilialId(c.filialId());
        e.setHashPlanilha(c.hashPlanilha());
        e.setNomeArquivo(c.nomeArquivo());
        e.setRegistrosProcessados(c.registrosProcessados());
        e.setRegistrosFalhos(c.registrosFalhos());
        e.setSolicitadoPor(c.solicitadoPor());
        e.setCreatedAt(c.createdAt());
        return e;
    }

    static CargaInicial toDomain(CargaInicialEntity e) {
        return new CargaInicial(
                CargaInicialId.of(e.getId()),
                e.getFilialId(), e.getHashPlanilha(), e.getNomeArquivo(),
                e.getRegistrosProcessados(), e.getRegistrosFalhos(),
                e.getSolicitadoPor(), e.getCreatedAt());
    }
}
