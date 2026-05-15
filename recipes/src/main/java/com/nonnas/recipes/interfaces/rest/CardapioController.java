package com.nonnas.recipes.interfaces.rest;

import com.nonnas.recipes.application.cardapio.ListarCardapioUseCase;
import com.nonnas.recipes.application.cardapio.VenderInsumoOrfaoUseCase;
import com.nonnas.recipes.interfaces.rest.dto.CardapioDto;
import com.nonnas.web.security.SecurityScope;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cardapio")
public class CardapioController {

    private final ListarCardapioUseCase listar;
    private final VenderInsumoOrfaoUseCase venderInsumo;

    public CardapioController(ListarCardapioUseCase listar, VenderInsumoOrfaoUseCase venderInsumo) {
        this.listar = listar;
        this.venderInsumo = venderInsumo;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public CardapioDto.Resposta listar(@RequestParam UUID filialId) {
        UUID escopo = SecurityScope.requireFilialId(filialId);
        return CardapioDto.Resposta.from(listar.execute(escopo));
    }

    @PostMapping("/vender-insumo")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public CardapioDto.VendaInsumoResposta venderInsumo(
            @Valid @RequestBody CardapioDto.VenderInsumoRequest req) {
        SecurityScope.assertCanAccess(req.filialId());
        var resp = venderInsumo.execute(new VenderInsumoOrfaoUseCase.Comando(
                req.insumoId(), req.filialId(), req.usuarioId(),
                req.quantidadeVendida(), req.observacao()));
        return new CardapioDto.VendaInsumoResposta(
                resp.produtoVendavelCriadoId(),
                resp.movimentacao().id().value(),
                resp.movimentacao().gerouNegativo());
    }
}
