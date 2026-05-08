package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.fornecedor.CriarFornecedorUseCase;
import com.nonnas.catalog.application.fornecedor.ListarFornecedoresUseCase;
import com.nonnas.catalog.interfaces.rest.dto.FornecedorDto;
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
@RequestMapping("/api/v1/fornecedores")
public class FornecedorController {

    private final CriarFornecedorUseCase criar;
    private final ListarFornecedoresUseCase listar;

    public FornecedorController(CriarFornecedorUseCase criar, ListarFornecedoresUseCase listar) {
        this.criar = criar;
        this.listar = listar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorDto.Response create(@Valid @RequestBody FornecedorDto.CreateRequest req) {
        return FornecedorDto.Response.from(criar.execute(req.razaoSocial(), req.cnpj()));
    }

    @GetMapping
    public List<FornecedorDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return listar.execute(page, size).stream().map(FornecedorDto.Response::from).toList();
    }
}
