package com.nonnas.reporting.interfaces.rest.dto;

import com.nonnas.reporting.domain.ClasseABC;
import com.nonnas.reporting.domain.CurvaABCItem;
import com.nonnas.reporting.domain.DivergenciaInventarioItem;
import com.nonnas.reporting.domain.MovimentacaoPorPeriodoItem;
import com.nonnas.reporting.domain.PosicaoEstoqueItem;
import com.nonnas.reporting.domain.RupturaIminenteItem;
import com.nonnas.reporting.domain.SituacaoRuptura;
import com.nonnas.reporting.domain.VencimentoProximoItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class RelatorioDto {

    public record PosicaoResponse(
            UUID filialId, UUID insumoId, String codigo, String nome,
            BigDecimal saldoTotal, BigDecimal valorEstoque, long quantidadeLotes
    ) {
        public static PosicaoResponse from(PosicaoEstoqueItem i) {
            return new PosicaoResponse(
                    i.filialId(), i.insumoId(), i.insumoCodigo(), i.insumoNome(),
                    i.saldoTotal(), i.valorEstoque(), i.quantidadeLotes());
        }
    }

    public record CurvaABCResponse(
            UUID filialId, UUID insumoId, String codigo, String nome,
            BigDecimal quantidadeTotal, BigDecimal valorTotal,
            BigDecimal percentualAcumulado, ClasseABC classe
    ) {
        public static CurvaABCResponse from(CurvaABCItem i) {
            return new CurvaABCResponse(
                    i.filialId(), i.insumoId(), i.insumoCodigo(), i.insumoNome(),
                    i.quantidadeTotal(), i.valorTotal(), i.percentualAcumulado(), i.classe());
        }
    }

    public record RupturaResponse(
            UUID filialId, UUID insumoId, String codigo, String nome,
            BigDecimal saldoTotal, BigDecimal estoqueMinimo, BigDecimal pontoPedido,
            SituacaoRuptura situacao
    ) {
        public static RupturaResponse from(RupturaIminenteItem i) {
            return new RupturaResponse(
                    i.filialId(), i.insumoId(), i.insumoCodigo(), i.insumoNome(),
                    i.saldoTotal(), i.estoqueMinimo(), i.pontoPedido(), i.situacao());
        }
    }

    public record VencimentoResponse(
            UUID filialId, UUID insumoId, UUID loteId,
            String codigo, String nome, String numeroLote,
            LocalDate dataValidade, long diasParaVencer,
            BigDecimal saldo, BigDecimal valorUnitario
    ) {
        public static VencimentoResponse from(VencimentoProximoItem i) {
            return new VencimentoResponse(
                    i.filialId(), i.insumoId(), i.loteId(),
                    i.insumoCodigo(), i.insumoNome(), i.numeroLote(),
                    i.dataValidade(), i.diasParaVencer(), i.saldo(), i.valorUnitario());
        }
    }

    public record MovimentacaoResponse(
            UUID filialId, UUID insumoId, String codigo, String nome,
            String tipoMovimentacao, long quantidadeMovimentacoes,
            BigDecimal quantidadeTotal, BigDecimal valorTotal
    ) {
        public static MovimentacaoResponse from(MovimentacaoPorPeriodoItem i) {
            return new MovimentacaoResponse(
                    i.filialId(), i.insumoId(), i.insumoCodigo(), i.insumoNome(),
                    i.tipoMovimentacao(), i.quantidadeMovimentacoes(),
                    i.quantidadeTotal(), i.valorTotal());
        }
    }

    public record DivergenciaResponse(
            UUID filialId, UUID insumoId, String codigo, String nome,
            long quantidadeAjustes,
            BigDecimal quantidadeDiffPositiva,
            BigDecimal quantidadeDiffNegativa,
            BigDecimal quantidadeDiffLiquida
    ) {
        public static DivergenciaResponse from(DivergenciaInventarioItem i) {
            return new DivergenciaResponse(
                    i.filialId(), i.insumoId(), i.insumoCodigo(), i.insumoNome(),
                    i.quantidadeAjustes(),
                    i.quantidadeDiffPositiva(), i.quantidadeDiffNegativa(),
                    i.quantidadeDiffLiquida());
        }
    }

    private RelatorioDto() {}
}
