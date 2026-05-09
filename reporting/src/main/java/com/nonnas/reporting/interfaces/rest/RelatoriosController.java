package com.nonnas.reporting.interfaces.rest;

import com.nonnas.reporting.application.CurvaABCUseCase;
import com.nonnas.reporting.application.DivergenciaInventarioUseCase;
import com.nonnas.reporting.application.MovimentacaoPorPeriodoUseCase;
import com.nonnas.reporting.application.PosicaoEstoquePorFilialUseCase;
import com.nonnas.reporting.application.RefreshViewsUseCase;
import com.nonnas.reporting.application.RupturaIminenteUseCase;
import com.nonnas.reporting.application.VencimentoProximoUseCase;
import com.nonnas.reporting.domain.PeriodoFiltro;
import com.nonnas.reporting.interfaces.rest.dto.RelatorioDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatoriosController {

    private final PosicaoEstoquePorFilialUseCase posicao;
    private final CurvaABCUseCase curvaAbc;
    private final RupturaIminenteUseCase ruptura;
    private final VencimentoProximoUseCase vencimento;
    private final MovimentacaoPorPeriodoUseCase movimentacao;
    private final DivergenciaInventarioUseCase divergencia;
    private final RefreshViewsUseCase refresh;

    public RelatoriosController(PosicaoEstoquePorFilialUseCase posicao,
                                CurvaABCUseCase curvaAbc,
                                RupturaIminenteUseCase ruptura,
                                VencimentoProximoUseCase vencimento,
                                MovimentacaoPorPeriodoUseCase movimentacao,
                                DivergenciaInventarioUseCase divergencia,
                                RefreshViewsUseCase refresh) {
        this.posicao = posicao;
        this.curvaAbc = curvaAbc;
        this.ruptura = ruptura;
        this.vencimento = vencimento;
        this.movimentacao = movimentacao;
        this.divergencia = divergencia;
        this.refresh = refresh;
    }

    @GetMapping("/posicao")
    public List<RelatorioDto.PosicaoResponse> posicao(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        var filtros = new PosicaoEstoquePorFilialUseCase.Filtros(filialId, categoriaId);
        return posicao.execute(filtros, page, size).stream()
                .map(RelatorioDto.PosicaoResponse::from).toList();
    }

    @GetMapping("/curva-abc")
    public List<RelatorioDto.CurvaABCResponse> curvaAbc(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return curvaAbc.execute(filialId, page, size).stream()
                .map(RelatorioDto.CurvaABCResponse::from).toList();
    }

    @GetMapping("/ruptura")
    public List<RelatorioDto.RupturaResponse> ruptura(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ruptura.execute(filialId, page, size).stream()
                .map(RelatorioDto.RupturaResponse::from).toList();
    }

    @GetMapping("/vencimento")
    public List<RelatorioDto.VencimentoResponse> vencimento(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(required = false) Integer diasJanela,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return vencimento.execute(filialId, diasJanela, page, size).stream()
                .map(RelatorioDto.VencimentoResponse::from).toList();
    }

    @GetMapping("/movimentacoes")
    public List<RelatorioDto.MovimentacaoResponse> movimentacoes(
            @RequestParam(required = false) UUID filialId,
            @RequestParam Instant inicio,
            @RequestParam Instant fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        var filtros = new MovimentacaoPorPeriodoUseCase.Filtros(
                filialId, new PeriodoFiltro(inicio, fim), tipo);
        return movimentacao.execute(filtros, page, size).stream()
                .map(RelatorioDto.MovimentacaoResponse::from).toList();
    }

    @GetMapping("/divergencia")
    public List<RelatorioDto.DivergenciaResponse> divergencia(
            @RequestParam(required = false) UUID filialId,
            @RequestParam Instant inicio,
            @RequestParam Instant fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return divergencia.execute(filialId, new PeriodoFiltro(inicio, fim), page, size).stream()
                .map(RelatorioDto.DivergenciaResponse::from).toList();
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void refresh() {
        refresh.execute();
    }
}
