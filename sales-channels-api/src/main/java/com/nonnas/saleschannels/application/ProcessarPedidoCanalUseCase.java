package com.nonnas.saleschannels.application;

import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.recipes.application.venda.RegistrarVendaSimuladaUseCase;
import com.nonnas.saleschannels.application.opendelivery.PedidoCanonicoMapper;
import com.nonnas.saleschannels.application.opendelivery.PedidoVendaCanonico;
import com.nonnas.saleschannels.application.ports.CanalAdapter;
import com.nonnas.saleschannels.application.ports.CanalProdutoDeParaRepository;
import com.nonnas.saleschannels.application.ports.CredencialCanalRepository;
import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.application.ports.PedidoCanalRepository;
import com.nonnas.saleschannels.domain.CanalProdutoDePara;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.domain.ItemPedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanalId;
import com.nonnas.saleschannels.domain.TipoEventoCanal;
import com.nonnas.sharedkernel.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Núcleo de baixa de estoque a partir de um pedido recebido por canal.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>{@link #processarEventoNaoProcessado(UUID, UUID)} é chamado pelo
 *       scheduler (T-CANAL-03) ou via endpoint manual para cada
 *       {@link EventoCanal} novo.</li>
 *   <li>Se for {@link TipoEventoCanal#PEDIDO_CRIADO}, busca o pedido
 *       canônico via {@link CanalAdapter#buscarPedido(String)}, mapeia
 *       para domain, persiste, e dispara
 *       {@link #processarPedido(PedidoCanalId, UUID)} (mesma transação).</li>
 *   <li>Para cada item: resolve {@code externalCode → ProdutoVendavel} via
 *       {@link CanalProdutoDeParaRepository}. Itens sem mapeamento ainda
 *       NÃO param o pedido — registramos um log e seguimos (operador
 *       resolve via UI). Em iteração futura podemos mover pra FALHA dura.</li>
 *   <li>Cada item resolvido vira chamada para
 *       {@link RegistrarVendaSimuladaUseCase} — gera uma {@link Movimentacao}
 *       SAIDA_VENDA por item (FABRICADO via ficha técnica; REVENDA 1:1).</li>
 *   <li>Marca a primeira {@code movimentacaoId} no pedido (audit trail
 *       cruzado fica em {@code movimentacao.documento_origem} via doc
 *       tipos existentes; campo dedicado pra canal é follow-up).</li>
 *   <li>Confirma no canal via {@link CanalAdapter#confirmarPedido(String)}
 *       — se a confirmação falhar, propaga e o pedido fica
 *       CONFIRMADO_ESTOQUE (estoque já baixou; operador re-tenta).</li>
 *   <li>Marca o evento como processado (idempotência: chamadas repetidas
 *       no mesmo evento são rejeitadas).</li>
 * </ol>
 *
 * <p>{@code usuarioSistemaId} é passado de fora — uma conta técnica
 * "canal-{tipo}" provisionada na identity. Em produção, o caller
 * (scheduler/endpoint) lê de configuração.
 */
@Service
public class ProcessarPedidoCanalUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarPedidoCanalUseCase.class);

    private final EventoCanalRepository eventos;
    private final PedidoCanalRepository pedidos;
    private final CredencialCanalRepository credenciais;
    private final CanalProdutoDeParaRepository deparaRepo;
    private final List<CanalAdapter> adaptersList;
    private final RegistrarVendaSimuladaUseCase vendaUseCase;
    private final com.fasterxml.jackson.databind.ObjectMapper jackson;
    private final Clock clock;

    public ProcessarPedidoCanalUseCase(EventoCanalRepository eventos,
                                       PedidoCanalRepository pedidos,
                                       CredencialCanalRepository credenciais,
                                       CanalProdutoDeParaRepository deparaRepo,
                                       List<CanalAdapter> adaptersList,
                                       RegistrarVendaSimuladaUseCase vendaUseCase,
                                       com.fasterxml.jackson.databind.ObjectMapper jackson,
                                       Clock clock) {
        this.eventos = eventos;
        this.pedidos = pedidos;
        this.credenciais = credenciais;
        this.deparaRepo = deparaRepo;
        this.adaptersList = adaptersList;
        this.vendaUseCase = vendaUseCase;
        this.jackson = jackson;
        this.clock = clock;
    }

    private Map<CanalTipo, CanalAdapter> adapters() {
        return adaptersList.stream()
                .collect(Collectors.toUnmodifiableMap(CanalAdapter::canal, Function.identity()));
    }

    /**
     * Processa um evento já persistido. Retorna o pedido (criado ou
     * atualizado) ou {@code null} se o evento foi ignorado (já processado,
     * tipo não-acionável, etc).
     */
    @Transactional
    public PedidoCanal processarEventoNaoProcessado(UUID eventoId, UUID usuarioSistemaId) {
        EventoCanal evento = eventos.findById(
                        new com.nonnas.saleschannels.domain.EventoCanalId(eventoId))
                .orElseThrow(() -> new NotFoundException("EventoCanal", eventoId));

        if (evento.processadoEmOpt().isPresent()) {
            log.debug("Evento {} já processado em {}, ignorando", eventoId, evento.processadoEmOpt().get());
            return null;
        }

        try {
            PedidoCanal resultado = switch (evento.tipoEvento()) {
                case PEDIDO_CRIADO -> materializarPedidoNovo(evento, usuarioSistemaId);
                case PEDIDO_CANCELADO -> cancelarPedidoExistente(evento);
                case PEDIDO_CONFIRMADO, PEDIDO_DESPACHADO, PEDIDO_CONCLUIDO, OUTRO -> {
                    log.debug("Evento {} tipo {} não aciona processamento", eventoId, evento.tipoEvento());
                    yield null;
                }
            };
            evento.marcarProcessado(clock.instant());
            eventos.atualizar(evento);
            return resultado;
        } catch (RuntimeException ex) {
            evento.marcarErro(ex.getMessage(), clock.instant());
            eventos.atualizar(evento);
            throw ex;
        }
    }

    private PedidoCanal materializarPedidoNovo(EventoCanal evento, UUID usuarioSistemaId) {
        // Idempotência cross-evento: se outro evento PLC já criou o pedido,
        // só associamos este evento ao pedido existente.
        String pedidoExterno = evento.pedidoExternoIdOpt()
                .orElseThrow(() -> new IllegalStateException(
                        "Evento PEDIDO_CRIADO sem pedidoExternoId: " + evento.id().value()));

        var existente = pedidos.findByCanalEPedidoExterno(evento.canalTipo(), pedidoExterno);
        if (existente.isPresent()) {
            evento.vincularPedido(existente.get().id());
            return processarPedidoSeAplicavel(existente.get(), usuarioSistemaId);
        }

        CanalAdapter adapter = adapters().get(evento.canalTipo());
        if (adapter == null) {
            throw new IllegalStateException("Sem adapter registrado para canal " + evento.canalTipo());
        }

        PedidoVendaCanonico canonico = adapter.buscarPedido(pedidoExterno);

        CredencialCanal credencial = credenciais.findAtivaByMerchantExterno(
                        evento.canalTipo(),
                        canonico.merchant() != null ? canonico.merchant().id() : "")
                .orElseThrow(() -> new IllegalStateException(
                        "Pedido " + pedidoExterno + " recebido para merchant não-cadastrado: "
                                + (canonico.merchant() != null ? canonico.merchant().id() : "<sem merchant>")));

        PedidoCanal novo = PedidoCanonicoMapper.paraDominio(
                canonico, evento.canalTipo(), credencial.filialId(), credencial.id(), clock.instant());

        String payloadCanonico = serializar(canonico);
        PedidoCanal persistido = pedidos.salvarNovo(novo, payloadCanonico, evento.payloadJson());
        evento.vincularPedido(persistido.id());

        return processarPedidoSeAplicavel(persistido, usuarioSistemaId);
    }

    private PedidoCanal cancelarPedidoExistente(EventoCanal evento) {
        String pedidoExterno = evento.pedidoExternoIdOpt().orElse(null);
        if (pedidoExterno == null) return null;

        return pedidos.findByCanalEPedidoExterno(evento.canalTipo(), pedidoExterno)
                .map(p -> {
                    if (p.status() != com.nonnas.saleschannels.domain.StatusPedidoCanal.CONCLUIDO
                            && p.status() != com.nonnas.saleschannels.domain.StatusPedidoCanal.CANCELADO) {
                        p.cancelar(clock.instant());
                        evento.vincularPedido(p.id());
                        return pedidos.atualizar(p);
                    }
                    return p;
                })
                .orElse(null);
    }

    /**
     * Dispara baixa de estoque + confirmação se o pedido está em
     * {@code RECEBIDO}. Exposto para reprocessamento manual (após operador
     * resolver de-para pendente, por exemplo).
     */
    @Transactional
    public PedidoCanal processarPedido(PedidoCanalId pedidoId, UUID usuarioSistemaId) {
        PedidoCanal pedido = pedidos.findById(pedidoId)
                .orElseThrow(() -> new NotFoundException("PedidoCanal", pedidoId.value()));
        return processarPedidoSeAplicavel(pedido, usuarioSistemaId);
    }

    private PedidoCanal processarPedidoSeAplicavel(PedidoCanal pedido, UUID usuarioSistemaId) {
        if (pedido.status() != com.nonnas.saleschannels.domain.StatusPedidoCanal.RECEBIDO) {
            return pedido;
        }

        pedido.iniciarProcessamento(clock.instant());
        pedidos.atualizar(pedido);

        // Resolve de-para por item. Cada item ganha produtoVendavelId
        // populado in-place no aggregate.
        for (ItemPedidoCanal item : pedido.itens()) {
            if (item.produtoVendavelIdOpt().isPresent()) continue;
            String externalCode = item.externalCodeOpt().orElse(null);
            if (externalCode == null) {
                throw new IllegalStateException(
                        "Item " + item.sequencia() + " do pedido " + pedido.pedidoExternoId()
                                + " sem externalCode — mapeamento de-para impossível");
            }
            CanalProdutoDePara depara = deparaRepo.resolver(
                            pedido.canalTipo(), externalCode, pedido.filialId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Sem de-para cadastrado para canal=" + pedido.canalTipo()
                                    + ", externalCode=" + externalCode
                                    + " (item seq " + item.sequencia() + ")"));
            item.resolverProdutoVendavel(depara.produtoVendavelId());
        }

        // Baixa estoque item a item. Captura a primeira Movimentação como
        // referência do pedido (audit completo via documento_origem dos
        // movs em recipes).
        UUID primeiraMovId = null;
        for (ItemPedidoCanal item : pedido.itens()) {
            UUID prodId = item.produtoVendavelIdOpt().orElseThrow();
            Movimentacao mov = vendaUseCase.execute(new RegistrarVendaSimuladaUseCase.Comando(
                    prodId, pedido.filialId(), usuarioSistemaId,
                    item.quantidade(),
                    "Canal " + pedido.canalTipo() + " pedido " + pedido.pedidoExternoId()
                            + " item " + item.sequencia()));
            if (primeiraMovId == null) primeiraMovId = mov.id().value();
        }

        pedido.confirmarEstoque(Objects.requireNonNull(primeiraMovId), clock.instant());
        pedidos.atualizar(pedido);

        // Confirma no canal. Se falhar, deixamos CONFIRMADO_ESTOQUE — operador
        // retenta pela tela. Não revertemos estoque (a baixa de fato ocorreu).
        try {
            CanalAdapter adapter = adapters().get(pedido.canalTipo());
            if (adapter != null) {
                adapter.confirmarPedido(pedido.pedidoExternoId());
            }
        } catch (RuntimeException ex) {
            log.warn("Confirmação no canal {} falhou para pedido {}: {}",
                    pedido.canalTipo(), pedido.pedidoExternoId(), ex.getMessage());
            // não propaga — estoque foi baixado com sucesso, ack é eventual
            return pedido;
        }

        pedido.concluir(clock.instant());
        return pedidos.atualizar(pedido);
    }

    private String serializar(Object obj) {
        try {
            return jackson.writeValueAsString(obj);
        } catch (Exception ex) {
            log.warn("Falha serializando payload canônico, salvando como '{}'", "{}", ex);
            return "{}";
        }
    }
}
