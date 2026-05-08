package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.empresa.CriarEmpresaUseCase;
import com.nonnas.identity.application.empresa.ListarEmpresasUseCase;
import com.nonnas.identity.interfaces.rest.dto.EmpresaDto;
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

@RestController
@RequestMapping("/api/v1/empresas")
public class EmpresaController {

    private final CriarEmpresaUseCase criar;
    private final ListarEmpresasUseCase listar;

    public EmpresaController(CriarEmpresaUseCase criar, ListarEmpresasUseCase listar) {
        this.criar = criar;
        this.listar = listar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EmpresaDto.Response create(@Valid @RequestBody EmpresaDto.CreateRequest req) {
        return EmpresaDto.Response.from(criar.execute(req.razaoSocial(), req.cnpj()));
    }

    @GetMapping
    public List<EmpresaDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return listar.execute(page, size).stream().map(EmpresaDto.Response::from).toList();
    }
}
