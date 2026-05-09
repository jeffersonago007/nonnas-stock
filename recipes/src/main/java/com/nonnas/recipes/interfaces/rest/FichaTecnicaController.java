package com.nonnas.recipes.interfaces.rest;

import com.nonnas.recipes.application.ficha.AtualizarFichaTecnicaUseCase;
import com.nonnas.recipes.application.ficha.BuscarFichaTecnicaVigenteUseCase;
import com.nonnas.recipes.application.ficha.CriarFichaTecnicaUseCase;
import com.nonnas.recipes.application.ficha.ListarHistoricoFichasUseCase;
import com.nonnas.recipes.interfaces.rest.dto.FichaTecnicaDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos-vendaveis/{produtoId}/fichas")
public class FichaTecnicaController {

    private final CriarFichaTecnicaUseCase criar;
    private final AtualizarFichaTecnicaUseCase atualizar;
    private final BuscarFichaTecnicaVigenteUseCase buscarVigente;
    private final ListarHistoricoFichasUseCase listarHistorico;

    public FichaTecnicaController(CriarFichaTecnicaUseCase criar,
                                  AtualizarFichaTecnicaUseCase atualizar,
                                  BuscarFichaTecnicaVigenteUseCase buscarVigente,
                                  ListarHistoricoFichasUseCase listarHistorico) {
        this.criar = criar;
        this.atualizar = atualizar;
        this.buscarVigente = buscarVigente;
        this.listarHistorico = listarHistorico;
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

    @GetMapping
    public List<FichaTecnicaDto.Response> historico(@PathVariable UUID produtoId) {
        return listarHistorico.execute(produtoId).stream()
                .map(FichaTecnicaDto.Response::from)
                .toList();
    }
}
