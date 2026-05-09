package com.nonnas.alerts.interfaces.rest;

import com.nonnas.alerts.application.config.AtualizarAlertaConfigUseCase;
import com.nonnas.alerts.application.config.CriarAlertaConfigUseCase;
import com.nonnas.alerts.application.ports.AlertaConfigRepository;
import com.nonnas.alerts.domain.AlertaConfigId;
import com.nonnas.alerts.interfaces.rest.dto.AlertaConfigDto;
import com.nonnas.sharedkernel.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public AlertaConfigDto.Response criar(@Valid @RequestBody AlertaConfigDto.CriarRequest req) {
        var cmd = new CriarAlertaConfigUseCase.Comando(
                req.tipo(), req.insumoId(), req.filialId(),
                req.threshold(), req.prioridade(), req.observacao());
        return AlertaConfigDto.Response.from(criar.execute(cmd));
    }

    @PutMapping("/{id}")
    public AlertaConfigDto.Response atualizar(@PathVariable UUID id,
                                              @Valid @RequestBody AlertaConfigDto.AtualizarRequest req) {
        var cmd = new AtualizarAlertaConfigUseCase.Comando(
                id, req.threshold(), req.prioridade(), req.observacao(), req.ativo());
        return AlertaConfigDto.Response.from(atualizar.execute(cmd));
    }

    @GetMapping("/{id}")
    public AlertaConfigDto.Response buscar(@PathVariable UUID id) {
        return repo.findById(AlertaConfigId.of(id))
                .map(AlertaConfigDto.Response::from)
                .orElseThrow(() -> new NotFoundException("Alerta config", id));
    }

    @GetMapping
    public List<AlertaConfigDto.Response> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return repo.findAll(page, size).stream()
                .map(AlertaConfigDto.Response::from).toList();
    }
}
