package com.nonnas.recipes.interfaces.rest;

import com.nonnas.recipes.application.ficha.AtualizarFichaTecnicaUseCase;
import com.nonnas.recipes.application.ficha.BuscarFichaTecnicaVigenteUseCase;
import com.nonnas.recipes.application.ficha.CriarFichaTecnicaUseCase;
import com.nonnas.recipes.interfaces.rest.dto.FichaTecnicaDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos-vendaveis/{produtoId}/fichas")
public class FichaTecnicaController {

    private final CriarFichaTecnicaUseCase criar;
    private final AtualizarFichaTecnicaUseCase atualizar;
    private final BuscarFichaTecnicaVigenteUseCase buscarVigente;

    public FichaTecnicaController(CriarFichaTecnicaUseCase criar,
                                  AtualizarFichaTecnicaUseCase atualizar,
                                  BuscarFichaTecnicaVigenteUseCase buscarVigente) {
        this.criar = criar;
        this.atualizar = atualizar;
        this.buscarVigente = buscarVigente;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FichaTecnicaDto.Response criar(@PathVariable UUID produtoId,
                                          @Valid @RequestBody FichaTecnicaDto.Request req) {
        var itens = req.itens().stream()
                .map(i -> new CriarFichaTecnicaUseCase.ItemEntrada(i.insumoId(), i.unidadeId(), i.quantidade()))
                .toList();
        var cmd = new CriarFichaTecnicaUseCase.Comando(produtoId, itens);
        return FichaTecnicaDto.Response.from(criar.execute(cmd));
    }

    @PutMapping("/vigente")
    public FichaTecnicaDto.Response atualizar(@PathVariable UUID produtoId,
                                              @Valid @RequestBody FichaTecnicaDto.Request req) {
        var itens = req.itens().stream()
                .map(i -> new AtualizarFichaTecnicaUseCase.ItemEntrada(i.insumoId(), i.unidadeId(), i.quantidade()))
                .toList();
        var cmd = new AtualizarFichaTecnicaUseCase.Comando(produtoId, itens);
        return FichaTecnicaDto.Response.from(atualizar.execute(cmd));
    }

    @GetMapping("/vigente")
    public FichaTecnicaDto.Response buscarVigente(@PathVariable UUID produtoId) {
        return FichaTecnicaDto.Response.from(buscarVigente.execute(produtoId));
    }
}
