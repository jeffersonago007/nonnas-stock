package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.categoria.AtivarCategoriaInsumoUseCase;
import com.nonnas.catalog.application.categoria.AtualizarCategoriaInsumoUseCase;
import com.nonnas.catalog.application.categoria.BuscarCategoriaInsumoUseCase;
import com.nonnas.catalog.application.categoria.CriarCategoriaInsumoUseCase;
import com.nonnas.catalog.application.categoria.DesativarCategoriaInsumoUseCase;
import com.nonnas.catalog.application.categoria.ListarCategoriasInsumoUseCase;
import com.nonnas.catalog.interfaces.rest.dto.CategoriaInsumoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/categorias-insumo")
public class CategoriaInsumoController {

    private final CriarCategoriaInsumoUseCase criar;
    private final ListarCategoriasInsumoUseCase listar;
    private final BuscarCategoriaInsumoUseCase buscar;
    private final AtualizarCategoriaInsumoUseCase atualizar;
    private final DesativarCategoriaInsumoUseCase desativar;
    private final AtivarCategoriaInsumoUseCase ativar;

    public CategoriaInsumoController(CriarCategoriaInsumoUseCase criar,
                                     ListarCategoriasInsumoUseCase listar,
                                     BuscarCategoriaInsumoUseCase buscar,
                                     AtualizarCategoriaInsumoUseCase atualizar,
                                     DesativarCategoriaInsumoUseCase desativar,
                                     AtivarCategoriaInsumoUseCase ativar) {
        this.criar = criar;
        this.listar = listar;
        this.buscar = buscar;
        this.atualizar = atualizar;
        this.desativar = desativar;
        this.ativar = ativar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public CategoriaInsumoDto.Response create(@Valid @RequestBody CategoriaInsumoDto.CreateRequest req) {
        return CategoriaInsumoDto.Response.from(criar.execute(req.nome(), req.categoriaPaiId()));
    }

    @GetMapping
    public List<CategoriaInsumoDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return listar.execute(page, size).stream().map(CategoriaInsumoDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    public CategoriaInsumoDto.Response getById(@PathVariable UUID id) {
        return CategoriaInsumoDto.Response.from(buscar.execute(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public CategoriaInsumoDto.Response update(@PathVariable UUID id,
                                              @Valid @RequestBody CategoriaInsumoDto.UpdateRequest req) {
        return CategoriaInsumoDto.Response.from(atualizar.execute(id, req.nome()));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public CategoriaInsumoDto.Response deactivate(@PathVariable UUID id) {
        return CategoriaInsumoDto.Response.from(desativar.execute(id));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public CategoriaInsumoDto.Response activate(@PathVariable UUID id) {
        return CategoriaInsumoDto.Response.from(ativar.execute(id));
    }
}
