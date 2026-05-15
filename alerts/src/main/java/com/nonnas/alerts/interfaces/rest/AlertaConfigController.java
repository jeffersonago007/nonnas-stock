package com.nonnas.alerts.interfaces.rest;

import com.nonnas.alerts.application.config.AtualizarAlertaConfigUseCase;
import com.nonnas.alerts.application.config.CriarAlertaConfigUseCase;
import com.nonnas.alerts.application.ports.AlertaConfigRepository;
import com.nonnas.alerts.domain.AlertaConfigId;
import com.nonnas.alerts.interfaces.rest.dto.AlertaConfigDto;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.web.security.SecurityScope;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alertas-config")
public class AlertaConfigController {

    private final CriarAlertaConfigUseCase criar;
    private final AtualizarAlertaConfigUseCase atualizar;
    private final AlertaConfigRepository repo;

    public AlertaConfigController(CriarAlertaConfigUseCase criar,
                                  AtualizarAlertaConfigUseCase atualizar,
                                  AlertaConfigRepository repo) {
        this.criar = criar;
        this.atualizar = atualizar;
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public AlertaConfigDto.Response criar(@Valid @RequestBody AlertaConfigDto.CriarRequest req) {
        // Não-admin: força a filial do usuário (ignora a requisitada para evitar IDOR).
        // Admin pode criar config global (filialId=null) ou para qualquer filial.
        UUID escopo = SecurityScope.isAdmin()
                ? req.filialId()
                : SecurityScope.resolveFilialId(req.filialId());
        var cmd = new CriarAlertaConfigUseCase.Comando(
                req.tipo(), req.insumoId(), escopo,
                req.threshold(), req.prioridade(), req.observacao());
        return AlertaConfigDto.Response.from(criar.execute(cmd));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public AlertaConfigDto.Response atualizar(@PathVariable UUID id,
                                              @Valid @RequestBody AlertaConfigDto.AtualizarRequest req) {
        assertConfigPertenceAoUsuario(id);
        var cmd = new AtualizarAlertaConfigUseCase.Comando(
                id, req.threshold(), req.prioridade(), req.observacao(), req.ativo());
        return AlertaConfigDto.Response.from(atualizar.execute(cmd));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AlertaConfigDto.Response buscar(@PathVariable UUID id) {
        var config = repo.findById(AlertaConfigId.of(id))
                .orElseThrow(() -> new NotFoundException("Alerta config", id));
        config.filialIdOpt().ifPresent(SecurityScope::assertCanAccess);
        return AlertaConfigDto.Response.from(config);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AlertaConfigDto.Response> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var todos = repo.findAll(page, size).stream();
        if (!SecurityScope.isAdmin()) {
            UUID mine = SecurityScope.currentFilialId().orElseThrow();
            todos = todos.filter(c -> c.filialIdOpt().map(mine::equals).orElse(true));
        }
        return todos.map(AlertaConfigDto.Response::from).toList();
    }

    private void assertConfigPertenceAoUsuario(UUID id) {
        var config = repo.findById(AlertaConfigId.of(id))
                .orElseThrow(() -> new NotFoundException("Alerta config", id));
        config.filialIdOpt().ifPresent(SecurityScope::assertCanAccess);
    }
}
