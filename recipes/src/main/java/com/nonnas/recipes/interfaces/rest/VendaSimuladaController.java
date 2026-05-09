package com.nonnas.recipes.interfaces.rest;

import com.nonnas.recipes.application.venda.RegistrarVendaSimuladaUseCase;
import com.nonnas.recipes.interfaces.rest.dto.VendaSimuladaDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendas-simuladas")
public class VendaSimuladaController {

    private final RegistrarVendaSimuladaUseCase registrar;

    public VendaSimuladaController(RegistrarVendaSimuladaUseCase registrar) {
        this.registrar = registrar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaSimuladaDto.Response registrar(@Valid @RequestBody VendaSimuladaDto.Request req) {
        var cmd = new RegistrarVendaSimuladaUseCase.Comando(
                req.produtoVendavelId(), req.filialId(), req.usuarioId(),
                req.quantidadeVendida(), req.observacao());
        return VendaSimuladaDto.Response.from(registrar.execute(cmd));
    }
}
