package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.categoria.CriarCategoriaInsumoUseCase;
import com.nonnas.catalog.application.categoria.ListarCategoriasInsumoUseCase;
import com.nonnas.catalog.interfaces.rest.dto.CategoriaInsumoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias-insumo")
public class CategoriaInsumoController {

    private final CriarCategoriaInsumoUseCase criar;
    private final ListarCategoriasInsumoUseCase listar;

    public CategoriaInsumoController(CriarCategoriaInsumoUseCase criar, ListarCategoriasInsumoUseCase listar) {
        this.criar = criar;
        this.listar = listar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaInsumoDto.Response create(@Valid @RequestBody CategoriaInsumoDto.CreateRequest req) {
        return CategoriaInsumoDto.Response.from(criar.execute(req.nome(), req.categoriaPaiId()));
    }

    @GetMapping
    public List<CategoriaInsumoDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return listar.execute(page, size).stream().map(CategoriaInsumoDto.Response::from).toList();
    }
}
