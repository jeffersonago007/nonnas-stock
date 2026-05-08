package com.nonnas.inventory.interfaces.rest;

import com.nonnas.inventory.application.saldo.ConsultarSaldoUseCase;
import com.nonnas.inventory.interfaces.rest.dto.MovimentacaoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/saldos")
public class SaldoController {

    private final ConsultarSaldoUseCase saldo;

    public SaldoController(ConsultarSaldoUseCase saldo) { this.saldo = saldo; }

    @GetMapping
    public MovimentacaoDto.SaldoResponse saldo(@RequestParam UUID insumoId, @RequestParam UUID filialId) {
        return new MovimentacaoDto.SaldoResponse(insumoId, filialId,
                saldo.saldoPorInsumoEFilial(insumoId, filialId));
    }
}
