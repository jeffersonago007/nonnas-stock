package com.nonnas.recipes.interfaces.rest;

import com.nonnas.recipes.application.venda.PreviewVendaSimuladaUseCase;
import com.nonnas.recipes.application.venda.RegistrarVendaSimuladaUseCase;
import com.nonnas.recipes.interfaces.rest.dto.VendaSimuladaDto;
import com.nonnas.web.security.SecurityScope;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendas-simuladas")
public class VendaSimuladaController {

    private final RegistrarVendaSimuladaUseCase registrar;
    private final PreviewVendaSimuladaUseCase preview;

    public VendaSimuladaController(RegistrarVendaSimuladaUseCase registrar,
                                   PreviewVendaSimuladaUseCase preview) {
        this.registrar = registrar;
        this.preview = preview;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public VendaSimuladaDto.Response registrar(@Valid @RequestBody VendaSimuladaDto.Request req) {
        SecurityScope.assertCanAccess(req.filialId());
        var cmd = new RegistrarVendaSimuladaUseCase.Comando(
                req.produtoVendavelId(), req.filialId(), req.usuarioId(),
                req.quantidadeVendida(), req.observacao());
        return VendaSimuladaDto.Response.from(registrar.execute(cmd));
    }

    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public VendaSimuladaDto.PreviewResponse preview(@Valid @RequestBody VendaSimuladaDto.PreviewRequest req) {
        SecurityScope.assertCanAccess(req.filialId());
        var cmd = new PreviewVendaSimuladaUseCase.Comando(
                req.produtoVendavelId(), req.filialId(), req.quantidadeVendida());
        return VendaSimuladaDto.PreviewResponse.from(preview.execute(cmd));
    }
}
