package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.insumo.AtivarInsumoUseCase;
import com.nonnas.catalog.application.insumo.AtualizarInsumoUseCase;
import com.nonnas.catalog.application.insumo.BuscarInsumoUseCase;
import com.nonnas.catalog.application.insumo.CriarInsumoUseCase;
import com.nonnas.catalog.application.insumo.DesativarInsumoUseCase;
import com.nonnas.catalog.application.insumo.ListarInsumosUseCase;
import com.nonnas.catalog.interfaces.rest.dto.InsumoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insumos")
public class InsumoController {

    private final CriarInsumoUseCase criar;
    private final ListarInsumosUseCase listar;
    private final BuscarInsumoUseCase buscar;
    private final AtualizarInsumoUseCase atualizar;
    private final DesativarInsumoUseCase desativar;
    private final AtivarInsumoUseCase ativar;

    public InsumoController(CriarInsumoUseCase criar,
                            ListarInsumosUseCase listar,
                            BuscarInsumoUseCase buscar,
                            AtualizarInsumoUseCase atualizar,
                            DesativarInsumoUseCase desativar,
                            AtivarInsumoUseCase ativar) {
        this.criar = criar;
        this.listar = listar;
        this.buscar = buscar;
        this.atualizar = atualizar;
        this.desativar = desativar;
        this.ativar = ativar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoDto.Response create(@Valid @RequestBody InsumoDto.CreateRequest req) {
        boolean cl = req.controlaLote() == null ? true : req.controlaLote();
        boolean cv = req.controlaValidade() == null ? true : req.controlaValidade();
        return InsumoDto.Response.from(criar.execute(req.codigo(), req.nome(),
                req.categoriaId(), req.unidadeBaseId(), cl, cv));
    }

    @GetMapping
    public List<InsumoDto.Response> list(@RequestParam(required = false) UUID categoriaId,
                                         @RequestParam(required = false) Boolean ativo,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        return listar.execute(categoriaId, ativo, q, page, size).stream().map(InsumoDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    public InsumoDto.Response getById(@PathVariable UUID id) {
        return InsumoDto.Response.from(buscar.execute(id));
    }

    @PutMapping("/{id}")
    public InsumoDto.Response update(@PathVariable UUID id,
                                     @Valid @RequestBody InsumoDto.UpdateRequest req) {
        return InsumoDto.Response.from(atualizar.execute(
                id, req.nome(), req.categoriaId(), req.controlaLote(),
                req.controlaValidade(), req.diasAlertaVencimento()));
    }

    @PatchMapping("/{id}/desativar")
    public InsumoDto.Response deactivate(@PathVariable UUID id) {
        return InsumoDto.Response.from(desativar.execute(id));
    }

    @PatchMapping("/{id}/ativar")
    public InsumoDto.Response activate(@PathVariable UUID id) {
        return InsumoDto.Response.from(ativar.execute(id));
    }
}
