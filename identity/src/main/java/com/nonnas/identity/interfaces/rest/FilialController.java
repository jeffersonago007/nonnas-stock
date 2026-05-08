package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.filial.CriarFilialUseCase;
import com.nonnas.identity.application.filial.ListarFiliaisUseCase;
import com.nonnas.identity.interfaces.rest.dto.FilialDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/filiais")
public class FilialController {

    private final CriarFilialUseCase criar;
    private final ListarFiliaisUseCase listar;

    public FilialController(CriarFilialUseCase criar, ListarFiliaisUseCase listar) {
        this.criar = criar;
        this.listar = listar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public FilialDto.Response create(@Valid @RequestBody FilialDto.CreateRequest req) {
        return FilialDto.Response.from(criar.execute(req.empresaId(), req.nome(), req.cnpj(), req.endereco()));
    }

    @GetMapping
    public List<FilialDto.Response> list(@RequestParam(required = false) UUID empresaId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return listar.execute(empresaId, page, size).stream().map(FilialDto.Response::from).toList();
    }
}
