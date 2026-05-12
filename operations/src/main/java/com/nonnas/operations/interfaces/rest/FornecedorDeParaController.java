package com.nonnas.operations.interfaces.rest;

import com.nonnas.operations.application.depara.ApagarDeParaUseCase;
import com.nonnas.operations.application.depara.ListarDeParasUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fornecedores/{fornecedorId}/de-para")
public class FornecedorDeParaController {

    private final ListarDeParasUseCase listar;
    private final ApagarDeParaUseCase apagar;

    public FornecedorDeParaController(ListarDeParasUseCase listar, ApagarDeParaUseCase apagar) {
        this.listar = listar;
        this.apagar = apagar;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<ListarDeParasUseCase.DeParaItem> listar(@PathVariable UUID fornecedorId) {
        return listar.execute(fornecedorId);
    }

    @DeleteMapping("/{codigoFornecedor}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable UUID fornecedorId, @PathVariable String codigoFornecedor) {
        apagar.execute(fornecedorId, codigoFornecedor);
    }
}
