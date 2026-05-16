package com.nonnas.saleschannels.interfaces.rest;

import com.nonnas.saleschannels.application.ports.CredencialCanalRepository;
import com.nonnas.saleschannels.application.ports.SegredoCifrador;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.CredencialCanalId;
import com.nonnas.saleschannels.interfaces.rest.dto.CredencialCanalDto;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/canais/credenciais")
@PreAuthorize("hasRole('ADMIN')")
class CredencialCanalController {

    private final CredencialCanalRepository repo;
    private final SegredoCifrador cifrador;
    private final Clock clock;

    CredencialCanalController(CredencialCanalRepository repo, SegredoCifrador cifrador, Clock clock) {
        this.repo = repo;
        this.cifrador = cifrador;
        this.clock = clock;
    }

    @GetMapping
    List<CredencialCanalDto.Response> listar(@RequestParam(value = "canal", required = false) CanalTipo canal) {
        List<CredencialCanal> credenciais = (canal != null) ? repo.listarPorCanal(canal) : repo.listarTodas();
        return credenciais.stream().map(CredencialCanalDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    CredencialCanalDto.Response buscar(@PathVariable UUID id) {
        return CredencialCanalDto.Response.from(repo.findById(CredencialCanalId.of(id))
                .orElseThrow(() -> new NotFoundException("Credencial", id)));
    }

    @PostMapping
    ResponseEntity<CredencialCanalDto.Response> criar(@Valid @RequestBody CredencialCanalDto.CreateRequest req) {
        repo.findAtivaByCanalEFilial(req.canalTipo(), req.filialId()).ifPresent(existente -> {
            throw new ValidationException(
                    "Já existe credencial ATIVA para canal " + req.canalTipo() + " e essa filial. " +
                    "Desative a anterior antes de criar nova.");
        });
        CredencialCanal nova = CredencialCanal.nova(
                req.canalTipo(), req.filialId(),
                req.merchantExternoId(), req.clientId(),
                cifrador.cifrar(req.clientSecret()),
                req.baseUrl(), req.observacao(), clock.instant());
        CredencialCanal salva = repo.save(nova);
        return ResponseEntity.status(HttpStatus.CREATED).body(CredencialCanalDto.Response.from(salva));
    }

    @PutMapping("/{id}")
    CredencialCanalDto.Response atualizar(@PathVariable UUID id, @RequestBody CredencialCanalDto.UpdateRequest req) {
        CredencialCanal c = repo.findById(CredencialCanalId.of(id))
                .orElseThrow(() -> new NotFoundException("Credencial", id));
        c.atualizarBaseUrl(req.baseUrl(), clock.instant());
        c.atualizarObservacao(req.observacao(), clock.instant());
        if (req.clientSecret() != null && !req.clientSecret().isBlank()) {
            c.rotacionarSegredo(cifrador.cifrar(req.clientSecret()), clock.instant());
        }
        return CredencialCanalDto.Response.from(repo.save(c));
    }

    @PatchMapping("/{id}/ativar")
    CredencialCanalDto.Response ativar(@PathVariable UUID id) {
        CredencialCanal c = repo.findById(CredencialCanalId.of(id))
                .orElseThrow(() -> new NotFoundException("Credencial", id));
        c.ativar(clock.instant());
        return CredencialCanalDto.Response.from(repo.save(c));
    }

    @PatchMapping("/{id}/desativar")
    CredencialCanalDto.Response desativar(@PathVariable UUID id) {
        CredencialCanal c = repo.findById(CredencialCanalId.of(id))
                .orElseThrow(() -> new NotFoundException("Credencial", id));
        c.desativar(clock.instant());
        return CredencialCanalDto.Response.from(repo.save(c));
    }
}
