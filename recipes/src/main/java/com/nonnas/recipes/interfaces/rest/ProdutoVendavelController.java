package com.nonnas.recipes.interfaces.rest;

import com.nonnas.recipes.application.produto.AtivarProdutoVendavelUseCase;
import com.nonnas.recipes.application.produto.AtualizarProdutoVendavelUseCase;
import com.nonnas.recipes.application.produto.CriarProdutoVendavelUseCase;
import com.nonnas.recipes.application.produto.DesativarProdutoVendavelUseCase;
import com.nonnas.recipes.application.produto.ListarProdutosVendaveisUseCase;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.interfaces.rest.dto.ProdutoVendavelDto;
import com.nonnas.sharedkernel.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos-vendaveis")
public class ProdutoVendavelController {

    private final CriarProdutoVendavelUseCase criar;
    private final ListarProdutosVendaveisUseCase listar;
    private final AtualizarProdutoVendavelUseCase atualizar;
    private final DesativarProdutoVendavelUseCase desativar;
    private final AtivarProdutoVendavelUseCase ativar;
    private final ProdutoVendavelRepository repo;

    public ProdutoVendavelController(CriarProdutoVendavelUseCase criar,
                                     ListarProdutosVendaveisUseCase listar,
                                     AtualizarProdutoVendavelUseCase atualizar,
                                     DesativarProdutoVendavelUseCase desativar,
                                     AtivarProdutoVendavelUseCase ativar,
                                     ProdutoVendavelRepository repo) {
        this.criar = criar;
        this.listar = listar;
        this.atualizar = atualizar;
        this.desativar = desativar;
        this.ativar = ativar;
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoVendavelDto.Response criar(@Valid @RequestBody ProdutoVendavelDto.Request req) {
        var cmd = new CriarProdutoVendavelUseCase.Comando(req.codigo(), req.nome(), req.categoria());
        return ProdutoVendavelDto.Response.from(criar.execute(cmd));
    }

    @GetMapping
    public List<ProdutoVendavelDto.Response> list(@RequestParam(required = false) String categoria,
                                                  @RequestParam(required = false) Boolean ativo,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return listar.execute(categoria, ativo, q, page, size).stream()
                .map(ProdutoVendavelDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    public ProdutoVendavelDto.Response buscar(@PathVariable UUID id) {
        return repo.findById(ProdutoVendavelId.of(id))
                .map(ProdutoVendavelDto.Response::from)
                .orElseThrow(() -> new NotFoundException("Produto vendável", id));
    }

    @PutMapping("/{id}")
    public ProdutoVendavelDto.Response update(@PathVariable UUID id,
                                              @Valid @RequestBody ProdutoVendavelDto.UpdateRequest req) {
        return ProdutoVendavelDto.Response.from(atualizar.execute(id, req.nome(), req.categoria()));
    }

    @PatchMapping("/{id}/desativar")
    public ProdutoVendavelDto.Response deactivate(@PathVariable UUID id) {
        return ProdutoVendavelDto.Response.from(desativar.execute(id));
    }

    @PatchMapping("/{id}/ativar")
    public ProdutoVendavelDto.Response activate(@PathVariable UUID id) {
        return ProdutoVendavelDto.Response.from(ativar.execute(id));
    }
}
