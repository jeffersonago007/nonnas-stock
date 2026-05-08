package com.nonnas.inventory.interfaces.rest;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.movimentacao.RegistrarSaidaManualUseCase;
import com.nonnas.inventory.interfaces.rest.dto.MovimentacaoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/movimentacoes")
public class MovimentacaoController {

    private final RegistrarEntradaManualUseCase entrada;
    private final RegistrarSaidaManualUseCase saida;

    public MovimentacaoController(RegistrarEntradaManualUseCase entrada, RegistrarSaidaManualUseCase saida) {
        this.entrada = entrada;
        this.saida = saida;
    }

    @PostMapping("/entrada-manual")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoDto.Response entrada(@Valid @RequestBody MovimentacaoDto.EntradaManualRequest req) {
        var cmd = new RegistrarEntradaManualUseCase.Comando(
                req.filialId(), req.usuarioId(), req.insumoId(), req.fornecedorId(), req.notaFiscalId(),
                req.numeroLote(), req.dataFabricacao(), req.dataValidade(), req.valorUnitario(),
                req.unidadeLancamentoId(), req.quantidadeLancada(), req.quantidadeBase(),
                req.tipo(), null, null, req.observacao());
        return MovimentacaoDto.Response.from(entrada.execute(cmd));
    }

    @PostMapping("/saida-manual")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoDto.Response saida(@Valid @RequestBody MovimentacaoDto.SaidaManualRequest req) {
        var cmd = new RegistrarSaidaManualUseCase.Comando(
                req.filialId(), req.usuarioId(), req.insumoId(), req.unidadeLancamentoId(),
                req.quantidadeBase(), req.tipo(), null, null, req.observacao());
        return MovimentacaoDto.Response.from(saida.execute(cmd));
    }
}
