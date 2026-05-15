package com.nonnas.operations.interfaces.rest;

import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.application.transferencia.AprovarTransferenciaUseCase;
import com.nonnas.operations.application.transferencia.CancelarTransferenciaUseCase;
import com.nonnas.operations.application.transferencia.ListarTransferenciasUseCase;
import com.nonnas.operations.application.transferencia.RegistrarEnvioTransferenciaUseCase;
import com.nonnas.operations.application.transferencia.RegistrarRecebimentoTransferenciaUseCase;
import com.nonnas.operations.application.transferencia.SolicitarTransferenciaUseCase;
import com.nonnas.operations.domain.StatusTransferencia;
import com.nonnas.operations.domain.Transferencia;
import com.nonnas.operations.domain.TransferenciaId;
import com.nonnas.operations.interfaces.rest.dto.TransferenciaDto;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.web.security.SecurityScope;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transferencias")
public class TransferenciaController {

    private final SolicitarTransferenciaUseCase solicitar;
    private final AprovarTransferenciaUseCase aprovar;
    private final RegistrarEnvioTransferenciaUseCase enviar;
    private final RegistrarRecebimentoTransferenciaUseCase receber;
    private final CancelarTransferenciaUseCase cancelar;
    private final ListarTransferenciasUseCase listar;
    private final TransferenciaRepository repo;

    public TransferenciaController(SolicitarTransferenciaUseCase solicitar,
                                   AprovarTransferenciaUseCase aprovar,
                                   RegistrarEnvioTransferenciaUseCase enviar,
                                   RegistrarRecebimentoTransferenciaUseCase receber,
                                   CancelarTransferenciaUseCase cancelar,
                                   ListarTransferenciasUseCase listar,
                                   TransferenciaRepository repo) {
        this.solicitar = solicitar;
        this.aprovar = aprovar;
        this.enviar = enviar;
        this.receber = receber;
        this.cancelar = cancelar;
        this.listar = listar;
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public TransferenciaDto.Response solicitar(@Valid @RequestBody TransferenciaDto.SolicitarRequest req) {
        assertEnvolveMinhaFilial(req.filialOrigemId(), req.filialDestinoId());
        var itens = req.itens().stream()
                .map(i -> new SolicitarTransferenciaUseCase.ItemEntrada(i.insumoId(), i.unidadeId(), i.quantidade()))
                .toList();
        var cmd = new SolicitarTransferenciaUseCase.Comando(
                req.filialOrigemId(), req.filialDestinoId(), req.solicitadoPor(),
                req.observacao(), itens);
        return TransferenciaDto.Response.from(solicitar.execute(cmd));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TransferenciaDto.Response buscar(@PathVariable UUID id) {
        Transferencia t = repo.findById(TransferenciaId.of(id))
                .orElseThrow(() -> new NotFoundException("Transferência", id));
        assertEnvolveMinhaFilial(t.filialOrigemId(), t.filialDestinoId());
        return TransferenciaDto.Response.from(t);
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public TransferenciaDto.Response aprovar(@PathVariable UUID id,
                                             @Valid @RequestBody TransferenciaDto.AcaoSimplesRequest req) {
        assertTransferenciaEnvolveMinhaFilial(id);
        return TransferenciaDto.Response.from(aprovar.execute(id, req.usuarioId()));
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public TransferenciaDto.Response enviar(@PathVariable UUID id,
                                            @Valid @RequestBody TransferenciaDto.AcaoSimplesRequest req) {
        assertTransferenciaEnvolveMinhaFilial(id);
        return TransferenciaDto.Response.from(enviar.execute(id, req.usuarioId()));
    }

    @PostMapping("/{id}/receber")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public TransferenciaDto.Response receber(@PathVariable UUID id,
                                             @Valid @RequestBody TransferenciaDto.RecebimentoRequest req) {
        assertTransferenciaEnvolveMinhaFilial(id);
        Map<UUID, BigDecimal> qtds = new HashMap<>();
        for (var ir : req.itens()) {
            qtds.put(ir.itemId(), ir.quantidadeRecebida());
        }
        var cmd = new RegistrarRecebimentoTransferenciaUseCase.Comando(id, req.recebidoPor(), qtds);
        return TransferenciaDto.Response.from(receber.execute(cmd));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public TransferenciaDto.Response cancelar(@PathVariable UUID id,
                                              @Valid @RequestBody TransferenciaDto.CancelarRequest req) {
        assertTransferenciaEnvolveMinhaFilial(id);
        return TransferenciaDto.Response.from(cancelar.execute(id, req.motivo()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TransferenciaDto.Response> list(@RequestParam(required = false) UUID filialId,
                                                @RequestParam(required = false) StatusTransferencia status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        UUID escopo = SecurityScope.resolveFilialId(filialId);
        return listar.execute(escopo, status, page, size).stream()
                .map(TransferenciaDto.Response::from).toList();
    }

    @GetMapping("/em-transito")
    @PreAuthorize("isAuthenticated()")
    public List<TransferenciaDto.EmTransitoResponse> emTransito(
            @RequestParam(required = false) UUID filialDestinoId) {
        UUID escopo = SecurityScope.resolveFilialId(filialDestinoId);
        return repo.agregadoEmTransito(escopo).stream()
                .map(r -> new TransferenciaDto.EmTransitoResponse(r.insumoId(), r.quantidadeEmTransito()))
                .toList();
    }

    /**
     * Não-admin só atua em transferências onde ele é dono da origem OU do destino.
     * Admin tem visão global.
     */
    private void assertEnvolveMinhaFilial(UUID origemId, UUID destinoId) {
        if (SecurityScope.isAdmin()) {
            return;
        }
        UUID minha = SecurityScope.currentFilialId()
                .orElseThrow(() -> new AccessDeniedException("Usuário não-ADMIN sem filial vinculada"));
        if (!minha.equals(origemId) && !minha.equals(destinoId)) {
            throw new AccessDeniedException("Transferência fora da sua filial");
        }
    }

    private void assertTransferenciaEnvolveMinhaFilial(UUID transferenciaId) {
        if (SecurityScope.isAdmin()) {
            return;
        }
        Transferencia t = repo.findById(TransferenciaId.of(transferenciaId))
                .orElseThrow(() -> new NotFoundException("Transferência", transferenciaId));
        assertEnvolveMinhaFilial(t.filialOrigemId(), t.filialDestinoId());
    }
}
