package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.insumo.CriarInsumoUseCase;
import com.nonnas.catalog.application.insumo.ListarInsumosUseCase;
import com.nonnas.catalog.interfaces.rest.dto.InsumoDto;
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
@RequestMapping("/api/v1/insumos")
public class InsumoController {

    private final CriarInsumoUseCase criar;
    private final ListarInsumosUseCase listar;

    public InsumoController(CriarInsumoUseCase criar, ListarInsumosUseCase listar) {
        this.criar = criar;
        this.listar = listar;
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
    public List<InsumoDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        return listar.execute(page, size).stream().map(InsumoDto.Response::from).toList();
    }
}
