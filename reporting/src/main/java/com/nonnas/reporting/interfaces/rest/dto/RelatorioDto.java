package com.nonnas.reporting.interfaces.rest.dto;

import com.nonnas.reporting.domain.ClasseABC;
import com.nonnas.reporting.domain.CmvPorCanalItem;
import com.nonnas.reporting.domain.CmvPorInsumoItem;
import com.nonnas.reporting.domain.CmvPorProdutoItem;
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

    public record CmvPorInsumoResponse(
            UUID insumoId, String codigo, String nome,
            BigDecimal quantidadeVendidaBase,
            BigDecimal cmvTotal,
            BigDecimal custoMedioPeriodo,
            long quantidadeMovimentacoes
    ) {
        public static CmvPorInsumoResponse from(CmvPorInsumoItem i) {
            return new CmvPorInsumoResponse(
                    i.insumoId(), i.codigo(), i.nome(),
                    i.quantidadeVendidaBase(), i.cmvTotal(),
                    i.custoMedioPeriodo(), i.quantidadeMovimentacoes());
        }
    }

    public record CmvPorProdutoResponse(
            UUID produtoVendavelId, String codigo, String nome,
            BigDecimal quantidadeVendida,
            BigDecimal cmvTotal,
            long quantidadeMovimentacoes
    ) {
        public static CmvPorProdutoResponse from(CmvPorProdutoItem i) {
            return new CmvPorProdutoResponse(
                    i.produtoVendavelId(), i.codigo(), i.nome(),
                    i.quantidadeVendida(), i.cmvTotal(), i.quantidadeMovimentacoes());
        }
    }

    public record CmvPorCanalResponse(
            String canal,
            long quantidadePedidos,
            BigDecimal receitaLiquidaTotal,
            BigDecimal cmvTotal,
            BigDecimal margemBruta
    ) {
        public static CmvPorCanalResponse from(CmvPorCanalItem i) {
            return new CmvPorCanalResponse(
                    i.canal(), i.quantidadePedidos(),
                    i.receitaLiquidaTotal(), i.cmvTotal(), i.margemBruta());
        }
    }

    private RelatorioDto() {}
}
