package com.nonnas.reporting.interfaces.rest;

import com.nonnas.reporting.application.CmvUseCase;
import com.nonnas.reporting.application.CurvaABCUseCase;
import com.nonnas.reporting.application.DivergenciaInventarioUseCase;
import com.nonnas.reporting.application.MovimentacaoPorPeriodoUseCase;
import com.nonnas.reporting.application.PosicaoEstoquePorFilialUseCase;
import com.nonnas.reporting.application.RefreshViewsUseCase;
import com.nonnas.reporting.application.RupturaIminenteUseCase;
import com.nonnas.reporting.application.VencimentoProximoUseCase;
import com.nonnas.reporting.domain.PeriodoFiltro;
import com.nonnas.reporting.interfaces.rest.dto.RelatorioDto;
import com.nonnas.web.security.SecurityScope;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final CmvUseCase cmv;

    public RelatoriosController(PosicaoEstoquePorFilialUseCase posicao,
                                CurvaABCUseCase curvaAbc,
                                RupturaIminenteUseCase ruptura,
                                VencimentoProximoUseCase vencimento,
                                MovimentacaoPorPeriodoUseCase movimentacao,
                                DivergenciaInventarioUseCase divergencia,
                                RefreshViewsUseCase refresh,
                                CmvUseCase cmv) {
        this.posicao = posicao;
        this.curvaAbc = curvaAbc;
        this.ruptura = ruptura;
        this.vencimento = vencimento;
        this.movimentacao = movimentacao;
        this.divergencia = divergencia;
        this.refresh = refresh;
        this.cmv = cmv;
    }

    @GetMapping("/posicao")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.PosicaoResponse> posicao(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        var filtros = new PosicaoEstoquePorFilialUseCase.Filtros(escopo, categoriaId);
        return posicao.execute(filtros, page, size).stream()
                .map(RelatorioDto.PosicaoResponse::from).toList();
    }

    @GetMapping("/curva-abc")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.CurvaABCResponse> curvaAbc(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        return curvaAbc.execute(escopo, page, size).stream()
                .map(RelatorioDto.CurvaABCResponse::from).toList();
    }

    @GetMapping("/ruptura")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.RupturaResponse> ruptura(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        return ruptura.execute(escopo, page, size).stream()
                .map(RelatorioDto.RupturaResponse::from).toList();
    }

    @GetMapping("/vencimento")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.VencimentoResponse> vencimento(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(required = false) Integer diasJanela,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        return vencimento.execute(escopo, diasJanela, page, size).stream()
                .map(RelatorioDto.VencimentoResponse::from).toList();
    }

    @GetMapping("/movimentacoes")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.MovimentacaoResponse> movimentacoes(
            @RequestParam(required = false) UUID filialId,
            @RequestParam Instant inicio,
            @RequestParam Instant fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        var filtros = new MovimentacaoPorPeriodoUseCase.Filtros(
                escopo, new PeriodoFiltro(inicio, fim), tipo);
        return movimentacao.execute(filtros, page, size).stream()
                .map(RelatorioDto.MovimentacaoResponse::from).toList();
    }

    @GetMapping("/divergencia")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.DivergenciaResponse> divergencia(
            @RequestParam(required = false) UUID filialId,
            @RequestParam Instant inicio,
            @RequestParam Instant fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        return divergencia.execute(escopo, new PeriodoFiltro(inicio, fim), page, size).stream()
                .map(RelatorioDto.DivergenciaResponse::from).toList();
    }

    @GetMapping("/cmv/por-insumo")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.CmvPorInsumoResponse> cmvPorInsumo(
            @RequestParam Instant de,
            @RequestParam Instant ate,
            @RequestParam(required = false) UUID filialId) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        return cmv.porInsumo(de, ate, escopo).stream()
                .map(RelatorioDto.CmvPorInsumoResponse::from).toList();
    }

    @GetMapping("/cmv/por-produto")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.CmvPorProdutoResponse> cmvPorProduto(
            @RequestParam Instant de,
            @RequestParam Instant ate,
            @RequestParam(required = false) UUID filialId) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        return cmv.porProduto(de, ate, escopo).stream()
                .map(RelatorioDto.CmvPorProdutoResponse::from).toList();
    }

    @GetMapping("/cmv/por-canal")
    @PreAuthorize("isAuthenticated()")
    public List<RelatorioDto.CmvPorCanalResponse> cmvPorCanal(
            @RequestParam Instant de,
            @RequestParam Instant ate,
            @RequestParam(required = false) UUID filialId) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        return cmv.porCanal(de, ate, escopo).stream()
                .map(RelatorioDto.CmvPorCanalResponse::from).toList();
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void refresh() {
        refresh.execute();
    }
}
