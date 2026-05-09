package com.nonnas.alerts.interfaces.rest;

import com.nonnas.alerts.application.disparado.ListarAlertasDisparadosUseCase;
import com.nonnas.alerts.application.disparado.MarcarAlertaResolvidoUseCase;
import com.nonnas.alerts.application.disparado.MarcarAlertaVisualizadoUseCase;
import com.nonnas.alerts.domain.StatusAlerta;
import com.nonnas.alerts.domain.TipoAlerta;
import com.nonnas.alerts.interfaces.rest.dto.AlertaDisparadoDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alertas-disparados")
public class AlertaDisparadoController {

    private final ListarAlertasDisparadosUseCase listar;
    private final MarcarAlertaResolvidoUseCase resolver;
    private final MarcarAlertaVisualizadoUseCase visualizar;

    public AlertaDisparadoController(ListarAlertasDisparadosUseCase listar,
                                     MarcarAlertaResolvidoUseCase resolver,
                                     MarcarAlertaVisualizadoUseCase visualizar) {
        this.listar = listar;
        this.resolver = resolver;
        this.visualizar = visualizar;
    }

    @GetMapping
    public List<AlertaDisparadoDto.Response> listar(
            @RequestParam(required = false) StatusAlerta status,
            @RequestParam(required = false) UUID filialId,
            @RequestParam(required = false) UUID insumoId,
            @RequestParam(required = false) TipoAlerta tipo,
            @RequestParam(required = false) Instant dataDisparoDe,
            @RequestParam(required = false) Instant dataDisparoAte,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var filtros = new ListarAlertasDisparadosUseCase.Filtros(
                status, filialId, insumoId, tipo, dataDisparoDe, dataDisparoAte);
        return listar.execute(filtros, page, size).stream()
                .map(AlertaDisparadoDto.Response::from).toList();
    }

    @PostMapping("/{id}/resolver")
    public AlertaDisparadoDto.Response resolver(@PathVariable UUID id,
                                                @Valid @RequestBody AlertaDisparadoDto.AcaoUsuarioRequest req) {
        return AlertaDisparadoDto.Response.from(resolver.execute(id, req.usuarioId()));
    }

    @PostMapping("/{id}/visualizar")
    public AlertaDisparadoDto.Response visualizar(@PathVariable UUID id,
                                                  @Valid @RequestBody AlertaDisparadoDto.AcaoUsuarioRequest req) {
        return AlertaDisparadoDto.Response.from(visualizar.execute(id, req.usuarioId()));
    }
}
