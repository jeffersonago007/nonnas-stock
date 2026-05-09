package com.nonnas.operations.interfaces.rest;

import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.application.transferencia.AprovarTransferenciaUseCase;
import com.nonnas.operations.application.transferencia.CancelarTransferenciaUseCase;
import com.nonnas.operations.application.transferencia.RegistrarEnvioTransferenciaUseCase;
import com.nonnas.operations.application.transferencia.RegistrarRecebimentoTransferenciaUseCase;
import com.nonnas.operations.application.transferencia.SolicitarTransferenciaUseCase;
import com.nonnas.operations.domain.TransferenciaId;
import com.nonnas.operations.interfaces.rest.dto.TransferenciaDto;
import com.nonnas.sharedkernel.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    private final TransferenciaRepository repo;

    public TransferenciaController(SolicitarTransferenciaUseCase solicitar,
                                   AprovarTransferenciaUseCase aprovar,
                                   RegistrarEnvioTransferenciaUseCase enviar,
                                   RegistrarRecebimentoTransferenciaUseCase receber,
                                   CancelarTransferenciaUseCase cancelar,
                                   TransferenciaRepository repo) {
        this.solicitar = solicitar;
        this.aprovar = aprovar;
        this.enviar = enviar;
        this.receber = receber;
        this.cancelar = cancelar;
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferenciaDto.Response solicitar(@Valid @RequestBody TransferenciaDto.SolicitarRequest req) {
        var itens = req.itens().stream()
                .map(i -> new SolicitarTransferenciaUseCase.ItemEntrada(i.insumoId(), i.unidadeId(), i.quantidade()))
                .toList();
        var cmd = new SolicitarTransferenciaUseCase.Comando(
                req.filialOrigemId(), req.filialDestinoId(), req.solicitadoPor(),
                req.observacao(), itens);
        return TransferenciaDto.Response.from(solicitar.execute(cmd));
    }

    @GetMapping("/{id}")
    public TransferenciaDto.Response buscar(@PathVariable UUID id) {
        return repo.findById(TransferenciaId.of(id))
                .map(TransferenciaDto.Response::from)
                .orElseThrow(() -> new NotFoundException("Transferência", id));
    }

    @PostMapping("/{id}/aprovar")
    public TransferenciaDto.Response aprovar(@PathVariable UUID id,
                                             @Valid @RequestBody TransferenciaDto.AcaoSimplesRequest req) {
        return TransferenciaDto.Response.from(aprovar.execute(id, req.usuarioId()));
    }

    @PostMapping("/{id}/enviar")
    public TransferenciaDto.Response enviar(@PathVariable UUID id,
                                            @Valid @RequestBody TransferenciaDto.AcaoSimplesRequest req) {
        return TransferenciaDto.Response.from(enviar.execute(id, req.usuarioId()));
    }

    @PostMapping("/{id}/receber")
    public TransferenciaDto.Response receber(@PathVariable UUID id,
                                             @Valid @RequestBody TransferenciaDto.RecebimentoRequest req) {
        Map<UUID, BigDecimal> qtds = new HashMap<>();
        for (var ir : req.itens()) {
            qtds.put(ir.itemId(), ir.quantidadeRecebida());
        }
        var cmd = new RegistrarRecebimentoTransferenciaUseCase.Comando(id, req.recebidoPor(), qtds);
        return TransferenciaDto.Response.from(receber.execute(cmd));
    }

    @PostMapping("/{id}/cancelar")
    public TransferenciaDto.Response cancelar(@PathVariable UUID id,
                                              @Valid @RequestBody TransferenciaDto.CancelarRequest req) {
        return TransferenciaDto.Response.from(cancelar.execute(id, req.motivo()));
    }

    @GetMapping("/em-transito")
    public List<TransferenciaDto.EmTransitoResponse> emTransito(
            @RequestParam(required = false) UUID filialDestinoId) {
        return repo.agregadoEmTransito(filialDestinoId).stream()
                .map(r -> new TransferenciaDto.EmTransitoResponse(r.insumoId(), r.quantidadeEmTransito()))
                .toList();
    }
}
