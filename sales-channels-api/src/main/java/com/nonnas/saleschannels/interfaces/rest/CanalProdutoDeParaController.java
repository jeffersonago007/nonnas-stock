package com.nonnas.saleschannels.interfaces.rest;

import com.nonnas.saleschannels.application.ports.CanalProdutoDeParaRepository;
import com.nonnas.saleschannels.domain.CanalProdutoDePara;
import com.nonnas.saleschannels.domain.CanalProdutoDeParaId;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.interfaces.rest.dto.CanalProdutoDeParaDto;
import com.nonnas.sharedkernel.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/canais/depara-produtos")
@PreAuthorize("hasRole('ADMIN')")
class CanalProdutoDeParaController {

    private final CanalProdutoDeParaRepository repo;
    private final Clock clock;

    CanalProdutoDeParaController(CanalProdutoDeParaRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @GetMapping
    List<CanalProdutoDeParaDto.Response> listar(@RequestParam("canal") CanalTipo canal) {
        return repo.listarPorCanal(canal).stream()
                .map(CanalProdutoDeParaDto.Response::from).toList();
    }

    @PostMapping
    ResponseEntity<CanalProdutoDeParaDto.Response> criar(@Valid @RequestBody CanalProdutoDeParaDto.CreateRequest req) {
        CanalProdutoDePara nova = CanalProdutoDePara.novo(
                req.canalTipo(), req.externalCode(),
                req.filialId(), req.produtoVendavelId(),
                req.observacao(), clock.instant());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CanalProdutoDeParaDto.Response.from(repo.save(nova)));
    }

    @PutMapping("/{id}")
    CanalProdutoDeParaDto.Response atualizar(@PathVariable UUID id,
                                              @Valid @RequestBody CanalProdutoDeParaDto.UpdateRequest req) {
        CanalProdutoDePara d = repo.findById(CanalProdutoDeParaId.of(id))
                .orElseThrow(() -> new NotFoundException("De-para", id));
        d.redirecionarProduto(req.produtoVendavelId(), clock.instant());
        d.atualizarObservacao(req.observacao(), clock.instant());
        return CanalProdutoDeParaDto.Response.from(repo.save(d));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deletar(@PathVariable UUID id) {
        repo.delete(CanalProdutoDeParaId.of(id));
        return ResponseEntity.noContent().build();
    }
}
