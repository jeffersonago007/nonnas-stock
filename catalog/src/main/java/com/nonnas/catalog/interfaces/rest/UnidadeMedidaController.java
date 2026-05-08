package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.unidademedida.CriarUnidadeMedidaUseCase;
import com.nonnas.catalog.application.unidademedida.ListarUnidadesMedidaUseCase;
import com.nonnas.catalog.interfaces.rest.dto.UnidadeMedidaDto;
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
@RequestMapping("/api/v1/unidades-medida")
public class UnidadeMedidaController {

    private final CriarUnidadeMedidaUseCase criar;
    private final ListarUnidadesMedidaUseCase listar;

    public UnidadeMedidaController(CriarUnidadeMedidaUseCase criar, ListarUnidadesMedidaUseCase listar) {
        this.criar = criar;
        this.listar = listar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnidadeMedidaDto.Response create(@Valid @RequestBody UnidadeMedidaDto.CreateRequest req) {
        return UnidadeMedidaDto.Response.from(criar.execute(req.codigo(), req.nome(), req.tipo()));
    }

    @GetMapping
    public List<UnidadeMedidaDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return listar.execute(page, size).stream().map(UnidadeMedidaDto.Response::from).toList();
    }
}
