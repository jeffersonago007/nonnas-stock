package com.nonnas.operations.interfaces.rest;

import com.nonnas.operations.application.ajuste.AprovarAjusteEstoqueUseCase;
import com.nonnas.operations.application.ajuste.LancarAjusteManualUseCase;
import com.nonnas.operations.application.ports.AjusteEstoqueRepository;
import com.nonnas.operations.domain.AjusteEstoqueId;
import com.nonnas.operations.interfaces.rest.dto.AjusteEstoqueDto;
import com.nonnas.sharedkernel.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public AjusteEstoqueDto.Response lancar(@Valid @RequestBody AjusteEstoqueDto.LancarRequest req) {
        var cmd = new LancarAjusteManualUseCase.Comando(
                req.filialId(), req.insumoId(), req.unidadeId(),
                req.quantidadeDiff(), req.motivo(), req.solicitadoPor());
        return AjusteEstoqueDto.Response.from(lancar.execute(cmd));
    }

    @GetMapping("/{id}")
    public AjusteEstoqueDto.Response buscar(@PathVariable UUID id) {
        return repo.findById(AjusteEstoqueId.of(id))
                .map(AjusteEstoqueDto.Response::from)
                .orElseThrow(() -> new NotFoundException("Ajuste de estoque", id));
    }

    @PostMapping("/{id}/aprovar")
    public AjusteEstoqueDto.Response aprovar(@PathVariable UUID id,
                                             @Valid @RequestBody AjusteEstoqueDto.AprovarRequest req) {
        return AjusteEstoqueDto.Response.from(aprovar.execute(id, req.aprovadoPor()));
    }
}
