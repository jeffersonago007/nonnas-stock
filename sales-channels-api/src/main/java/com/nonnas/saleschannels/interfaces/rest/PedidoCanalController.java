package com.nonnas.saleschannels.interfaces.rest;

import com.nonnas.saleschannels.application.ProcessarPedidoCanalUseCase;
import com.nonnas.saleschannels.application.ports.PedidoCanalRepository;
import com.nonnas.saleschannels.domain.PedidoCanalId;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;
import com.nonnas.saleschannels.interfaces.rest.dto.PedidoCanalDto;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.web.security.SecurityScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/canais/pedidos")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
class PedidoCanalController {

    private final PedidoCanalRepository repo;
    private final ProcessarPedidoCanalUseCase processarUseCase;

    PedidoCanalController(PedidoCanalRepository repo, ProcessarPedidoCanalUseCase processarUseCase) {
        this.repo = repo;
        this.processarUseCase = processarUseCase;
    }

    @GetMapping
    List<PedidoCanalDto.Response> listar(@RequestParam("filialId") UUID filialId,
                                          @RequestParam(value = "status", required = false) StatusPedidoCanal status) {
        return (status != null
                ? repo.listarPorFilialEStatus(filialId, status)
                : repo.listarPorFilial(filialId)
        ).stream().map(PedidoCanalDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    PedidoCanalDto.Response buscar(@PathVariable UUID id) {
        return PedidoCanalDto.Response.from(repo.findById(PedidoCanalId.of(id))
                .orElseThrow(() -> new NotFoundException("Pedido canal", id)));
    }

    /**
     * Re-processa um pedido travado (RECEBIDO ou EM_PROCESSAMENTO). Útil
     * depois de o operador resolver de-para pendente ou de o canal voltar
     * a aceitar confirmação. Idempotente: pedidos já concluídos/cancelados
     * são devolvidos como estão.
     */
    @PostMapping("/{id}/reprocessar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    PedidoCanalDto.Response reprocessar(@PathVariable UUID id) {
        return PedidoCanalDto.Response.from(processarUseCase.processarPedido(PedidoCanalId.of(id), SecurityScope.current().userId()));
    }
}
