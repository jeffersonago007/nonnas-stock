package com.nonnas.operations.interfaces.rest;

import com.nonnas.operations.application.ajuste.AprovarAjusteEstoqueUseCase;
import com.nonnas.operations.application.ajuste.LancarAjusteManualUseCase;
import com.nonnas.operations.application.ports.AjusteEstoqueRepository;
import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.domain.AjusteEstoqueId;
import com.nonnas.operations.interfaces.rest.dto.AjusteEstoqueDto;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.web.security.SecurityScope;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ajustes-estoque")
public class AjusteEstoqueController {

    private final LancarAjusteManualUseCase lancar;
    private final AprovarAjusteEstoqueUseCase aprovar;
    private final AjusteEstoqueRepository repo;

    public AjusteEstoqueController(LancarAjusteManualUseCase lancar,
                                   AprovarAjusteEstoqueUseCase aprovar,
                                   AjusteEstoqueRepository repo) {
        this.lancar = lancar;
        this.aprovar = aprovar;
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public AjusteEstoqueDto.Response lancar(@Valid @RequestBody AjusteEstoqueDto.LancarRequest req) {
        SecurityScope.assertCanAccess(req.filialId());
        var cmd = new LancarAjusteManualUseCase.Comando(
                req.filialId(), req.insumoId(), req.unidadeId(),
                req.quantidadeDiff(), req.motivo(), req.solicitadoPor());
        return AjusteEstoqueDto.Response.from(lancar.execute(cmd));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AjusteEstoqueDto.Response buscar(@PathVariable UUID id) {
        AjusteEstoque ajuste = repo.findById(AjusteEstoqueId.of(id))
                .orElseThrow(() -> new NotFoundException("Ajuste de estoque", id));
        SecurityScope.assertCanAccess(ajuste.filialId());
        return AjusteEstoqueDto.Response.from(ajuste);
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public AjusteEstoqueDto.Response aprovar(@PathVariable UUID id,
                                             @Valid @RequestBody AjusteEstoqueDto.AprovarRequest req) {
        AjusteEstoque ajuste = repo.findById(AjusteEstoqueId.of(id))
                .orElseThrow(() -> new NotFoundException("Ajuste de estoque", id));
        SecurityScope.assertCanAccess(ajuste.filialId());
        return AjusteEstoqueDto.Response.from(aprovar.execute(id, req.aprovadoPor()));
    }
}
