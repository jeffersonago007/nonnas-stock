package com.nonnas.saleschannels.interfaces.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.saleschannels.application.ports.CredencialCanalRepository;
import com.nonnas.saleschannels.application.ports.PedidoCanalRepository;
import com.nonnas.saleschannels.application.ports.SegredoCifrador;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.ItemPedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.interfaces.rest.dto.PedidoCanalDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Endpoint dev-only para gerar um pedido canal sem precisar de canal real
 * (iFood/Prism) rodando. Permite popular {@code /canais/pedidos} com
 * pedidos em estado RECEBIDO controlados pelo operador para demonstração
 * do pipeline ponta-a-ponta (de-para → baixa de estoque → movimentação →
 * saldo).
 *
 * <p>Diferente do fluxo real, este endpoint pula a parte de polling/adapter
 * HTTP e cria o {@link PedidoCanal} direto no estado RECEBIDO; o operador
 * clica "Reprocessar" na UI para disparar
 * {@link com.nonnas.saleschannels.application.ProcessarPedidoCanalUseCase}
 * exatamente como aconteceria com um pedido real.
 *
 * <p>{@code @Profile("dev")} garante que NÃO é registrado no contexto Spring
 * fora do perfil dev — em prod ele simplesmente não existe (404 silencioso).
 */
@Profile("dev")
@RestController
@RequestMapping("/api/v1/canais/dev")
@PreAuthorize("hasRole('ADMIN')")
class SimularPedidoDevController {

    private final PedidoCanalRepository pedidos;
    private final CredencialCanalRepository credenciais;
    private final SegredoCifrador cifrador;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    SimularPedidoDevController(PedidoCanalRepository pedidos,
                                CredencialCanalRepository credenciais,
                                SegredoCifrador cifrador,
                                ObjectMapper objectMapper,
                                Clock clock) {
        this.pedidos = pedidos;
        this.credenciais = credenciais;
        this.cifrador = cifrador;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Auto-cria credencial dev na primeira chamada por {@code (canal, filial)}.
     * Demo não precisa de OAuth real do iFood; o pipeline interno
     * (de-para → baixa de estoque) é o que importa. Marca {@code observacao}
     * pra deixar claro na lista que é fake.
     */
    private CredencialCanal autoCriarCredencialDev(CanalTipo canal, UUID filialId) {
        Instant agora = clock.instant();
        CredencialCanal nova = CredencialCanal.nova(
                canal, filialId,
                "dev-merchant-" + canal.name().toLowerCase(),
                "dev-client",
                cifrador.cifrar("dev-secret"),
                null,
                "Auto-criada pelo simulador dev — não usar em produção",
                agora);
        return credenciais.save(nova);
    }

    @PostMapping("/simular-pedido")
    ResponseEntity<PedidoCanalDto.Response> simular(@Valid @RequestBody SimularPedidoRequest req) {
        CredencialCanal credencial = credenciais.findAtivaByCanalEFilial(req.canal(), req.filialId())
                .orElseGet(() -> autoCriarCredencialDev(req.canal(), req.filialId()));

        Instant agora = clock.instant();
        String pedidoExternoId = "dev-" + UUID.randomUUID();
        String displayId = req.displayId() != null && !req.displayId().isBlank()
                ? req.displayId()
                : "DEMO-" + agora.toEpochMilli() % 100000;

        List<ItemPedidoCanal> itens = IntStream.range(0, req.itens().size())
                .mapToObj(i -> {
                    SimularItemRequest item = req.itens().get(i);
                    BigDecimal total = item.precoUnitario().multiply(item.quantidade());
                    return ItemPedidoCanal.novo(i + 1, item.externalCode(), item.nome(),
                            item.quantidade(), item.unidade(),
                            item.precoUnitario(), total, item.observacao());
                })
                .toList();

        BigDecimal subtotalItens = itens.stream()
                .map(ItemPedidoCanal::precoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxaEntrega = req.taxaEntrega() != null ? req.taxaEntrega() : BigDecimal.ZERO;
        BigDecimal taxaServico = req.taxaServico() != null ? req.taxaServico() : BigDecimal.ZERO;
        BigDecimal valorTotal = subtotalItens.add(taxaEntrega).add(taxaServico);
        BigDecimal valorLiquido = subtotalItens;

        PedidoCanal pedido = PedidoCanal.recebido(
                req.canal(), pedidoExternoId, displayId, req.filialId(),
                credencial.id(), valorTotal,
                taxaEntrega, taxaServico, valorLiquido,
                "BRL",
                req.clienteNome(), req.clienteTelefone(),
                itens, agora);

        String payloadJson = serializarPayload(req, pedidoExternoId, displayId, agora);
        PedidoCanal salvo = pedidos.salvarNovo(pedido, payloadJson, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoCanalDto.Response.from(salvo));
    }

    private String serializarPayload(SimularPedidoRequest req, String pedidoExternoId,
                                      String displayId, Instant agora) {
        // JSON mínimo representativo — não precisa ser PedidoVendaCanonico completo
        // porque o use case de processamento usa o domain (PedidoCanal/ItemPedidoCanal)
        // como fonte da verdade; o JSON serve só para auditoria/inspeção.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", pedidoExternoId);
        payload.put("displayId", displayId);
        payload.put("canal", req.canal().name());
        payload.put("createdAt", agora.toString());
        payload.put("origem", "DEV_SIMULADO");
        payload.put("itens", req.itens());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public record SimularPedidoRequest(
            @NotNull CanalTipo canal,
            @NotNull UUID filialId,
            String displayId,
            String clienteNome,
            String clienteTelefone,
            @PositiveOrZero BigDecimal taxaEntrega,
            @PositiveOrZero BigDecimal taxaServico,
            @NotNull @jakarta.validation.constraints.Size(min = 1, message = "Pedido precisa de ao menos 1 item")
            List<@Valid SimularItemRequest> itens
    ) {}

    public record SimularItemRequest(
            @NotBlank String externalCode,
            @NotBlank String nome,
            @NotNull @Positive BigDecimal quantidade,
            @NotBlank String unidade,
            @NotNull @PositiveOrZero BigDecimal precoUnitario,
            String observacao
    ) {}
}
