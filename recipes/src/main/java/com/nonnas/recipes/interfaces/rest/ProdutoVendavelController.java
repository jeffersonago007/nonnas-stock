package com.nonnas.recipes.interfaces.rest;

import com.nonnas.recipes.application.produto.CriarProdutoVendavelUseCase;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.interfaces.rest.dto.ProdutoVendavelDto;
import com.nonnas.sharedkernel.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos-vendaveis")
public class ProdutoVendavelController {

    private final CriarProdutoVendavelUseCase criar;
    private final ProdutoVendavelRepository repo;

    public ProdutoVendavelController(CriarProdutoVendavelUseCase criar, ProdutoVendavelRepository repo) {
        this.criar = criar;
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoVendavelDto.Response criar(@Valid @RequestBody ProdutoVendavelDto.Request req) {
        var cmd = new CriarProdutoVendavelUseCase.Comando(req.codigo(), req.nome(), req.categoria());
        return ProdutoVendavelDto.Response.from(criar.execute(cmd));
    }

    @GetMapping("/{id}")
    public ProdutoVendavelDto.Response buscar(@PathVariable UUID id) {
        return repo.findById(ProdutoVendavelId.of(id))
                .map(ProdutoVendavelDto.Response::from)
                .orElseThrow(() -> new NotFoundException("Produto vendável", id));
    }
}
