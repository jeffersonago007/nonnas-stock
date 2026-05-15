package com.nonnas.alerts.interfaces.rest;

import com.nonnas.alerts.application.disparado.ListarAlertasDisparadosUseCase;
import com.nonnas.alerts.application.disparado.MarcarAlertaResolvidoUseCase;
import com.nonnas.alerts.application.disparado.MarcarAlertaVisualizadoUseCase;
import com.nonnas.alerts.domain.AvaliadorAlertasService;
import com.nonnas.alerts.domain.StatusAlerta;
import com.nonnas.alerts.domain.TipoAlerta;
import com.nonnas.alerts.interfaces.rest.dto.AlertaDisparadoDto;
import com.nonnas.web.security.SecurityScope;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alertas-disparados")
public class AlertaDisparadoController {

    private final ListarAlertasDisparadosUseCase listar;
    private final MarcarAlertaResolvidoUseCase resolver;
    private final MarcarAlertaVisualizadoUseCase visualizar;
    private final AvaliadorAlertasService avaliador;

    public AlertaDisparadoController(ListarAlertasDisparadosUseCase listar,
                                     MarcarAlertaResolvidoUseCase resolver,
                                     MarcarAlertaVisualizadoUseCase visualizar,
                                     AvaliadorAlertasService avaliador) {
        this.listar = listar;
        this.resolver = resolver;
        this.visualizar = visualizar;
        this.avaliador = avaliador;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AlertaDisparadoDto.Response> listar(
            @RequestParam(required = false) StatusAlerta status,
            @RequestParam(required = false) UUID filialId,
            @RequestParam(required = false) UUID insumoId,
            @RequestParam(required = false) TipoAlerta tipo,
            @RequestParam(required = false) Instant dataDisparoDe,
            @RequestParam(required = false) Instant dataDisparoAte,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        var filtros = new ListarAlertasDisparadosUseCase.Filtros(
                status, escopo, insumoId, tipo, dataDisparoDe, dataDisparoAte);
        return listar.execute(filtros, page, size).stream()
                .map(AlertaDisparadoDto.Response::from).toList();
    }

    @PostMapping("/{id}/resolver")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public AlertaDisparadoDto.Response resolver(@PathVariable UUID id,
                                                @Valid @RequestBody AlertaDisparadoDto.AcaoUsuarioRequest req) {
        var resp = AlertaDisparadoDto.Response.from(resolver.execute(id, req.usuarioId()));
        SecurityScope.assertCanAccess(resp.filialId());
        return resp;
    }

    @PostMapping("/{id}/visualizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public AlertaDisparadoDto.Response visualizar(@PathVariable UUID id,
                                                  @Valid @RequestBody AlertaDisparadoDto.AcaoUsuarioRequest req) {
        var resp = AlertaDisparadoDto.Response.from(visualizar.execute(id, req.usuarioId()));
        SecurityScope.assertCanAccess(resp.filialId());
        return resp;
    }

    @PostMapping("/avaliar-vencimentos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Transactional
    public Map<String, Integer> avaliarVencimentos() {
        return Map.of("disparados", avaliador.avaliarVencimentos());
    }
}
