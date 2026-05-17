package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pedido recebido de um canal externo (iFood, 99Food, Keeta) já em forma
 * canônica Open Delivery. Aggregate root contendo itens e estado.
 *
 * <p>Transições permitidas:
 * <pre>
 *   RECEBIDO → EM_PROCESSAMENTO → CONFIRMADO_ESTOQUE → CONCLUIDO
 *   RECEBIDO | EM_PROCESSAMENTO → FALHA
 *   qualquer estado não-final → CANCELADO
 * </pre>
 *
 * <p>{@code pedidoExternoId} é a chave do canal — combinação {@code (canal,
 * pedidoExternoId)} é única no schema, prevenindo reentrada.
 */
public final class PedidoCanal {

    private final PedidoCanalId id;
    private final CanalTipo canalTipo;
    private final String pedidoExternoId;
    private final String displayId;
    private final UUID filialId;
    private final CredencialCanalId credencialId;
    private StatusPedidoCanal status;
    private final BigDecimal valorTotal;
    private final BigDecimal taxaEntrega;
    private final BigDecimal taxaServico;
    private final BigDecimal valorLiquido;
    private final String moeda;
    private final String clienteNome;
    private final String clienteTelefone;
    private final List<ItemPedidoCanal> itens;
    private UUID movimentacaoId;
    private String erroProcessamento;
    private final Instant recebidoEm;
    private Instant processadoEm;
    private Instant concluidoEm;
    private Instant canceladoEm;

    public PedidoCanal(PedidoCanalId id, CanalTipo canalTipo, String pedidoExternoId,
                       String displayId, UUID filialId, CredencialCanalId credencialId,
                       StatusPedidoCanal status, BigDecimal valorTotal,
                       BigDecimal taxaEntrega, BigDecimal taxaServico, BigDecimal valorLiquido,
                       String moeda,
                       String clienteNome, String clienteTelefone,
                       List<ItemPedidoCanal> itens, UUID movimentacaoId,
                       String erroProcessamento, Instant recebidoEm,
                       Instant processadoEm, Instant concluidoEm, Instant canceladoEm) {
        this.id = Objects.requireNonNull(id);
        this.canalTipo = Objects.requireNonNull(canalTipo);
        this.pedidoExternoId = exigir(pedidoExternoId, "pedidoExternoId");
        this.displayId = displayId;
        this.filialId = Objects.requireNonNull(filialId);
        this.credencialId = Objects.requireNonNull(credencialId);
        this.status = Objects.requireNonNull(status);
        this.valorTotal = exigirNaoNegativo(valorTotal, "valorTotal");
        this.taxaEntrega = exigirNaoNegativo(taxaEntrega, "taxaEntrega");
        this.taxaServico = exigirNaoNegativo(taxaServico, "taxaServico");
        this.valorLiquido = exigirNaoNegativo(valorLiquido, "valorLiquido");
        this.moeda = exigir(moeda, "moeda");
        this.clienteNome = clienteNome;
        this.clienteTelefone = clienteTelefone;
        Objects.requireNonNull(itens, "itens");
        if (itens.isEmpty()) {
            throw new ValidationException("pedido deve ter ao menos 1 item");
        }
        this.itens = new ArrayList<>(itens);
        this.movimentacaoId = movimentacaoId;
        this.erroProcessamento = erroProcessamento;
        this.recebidoEm = Objects.requireNonNull(recebidoEm);
        this.processadoEm = processadoEm;
        this.concluidoEm = concluidoEm;
        this.canceladoEm = canceladoEm;
    }

    public static PedidoCanal recebido(CanalTipo canalTipo, String pedidoExternoId,
                                       String displayId, UUID filialId,
                                       CredencialCanalId credencialId,
                                       BigDecimal valorTotal,
                                       BigDecimal taxaEntrega, BigDecimal taxaServico,
                                       BigDecimal valorLiquido,
                                       String moeda,
                                       String clienteNome, String clienteTelefone,
                                       List<ItemPedidoCanal> itens, Instant agora) {
        return new PedidoCanal(PedidoCanalId.generate(), canalTipo, pedidoExternoId,
                displayId, filialId, credencialId, StatusPedidoCanal.RECEBIDO,
                valorTotal, taxaEntrega, taxaServico, valorLiquido,
                moeda, clienteNome, clienteTelefone, itens,
                null, null, agora, null, null, null);
    }

    public void iniciarProcessamento(Instant agora) {
        if (status != StatusPedidoCanal.RECEBIDO) {
            throw new BusinessRuleException("só RECEBIDO pode iniciar processamento (atual: " + status + ")");
        }
        this.status = StatusPedidoCanal.EM_PROCESSAMENTO;
        this.processadoEm = agora;
    }

    public void confirmarEstoque(UUID movimentacaoId, Instant agora) {
        if (status != StatusPedidoCanal.EM_PROCESSAMENTO) {
            throw new BusinessRuleException("confirmarEstoque exige EM_PROCESSAMENTO (atual: " + status + ")");
        }
        this.movimentacaoId = Objects.requireNonNull(movimentacaoId);
        this.status = StatusPedidoCanal.CONFIRMADO_ESTOQUE;
        this.processadoEm = agora;
    }

    public void concluir(Instant agora) {
        if (status != StatusPedidoCanal.CONFIRMADO_ESTOQUE) {
            throw new BusinessRuleException("concluir exige CONFIRMADO_ESTOQUE (atual: " + status + ")");
        }
        this.status = StatusPedidoCanal.CONCLUIDO;
        this.concluidoEm = agora;
    }

    public void marcarFalha(String erro, Instant agora) {
        if (status != StatusPedidoCanal.RECEBIDO && status != StatusPedidoCanal.EM_PROCESSAMENTO) {
            throw new BusinessRuleException("falha só a partir de RECEBIDO ou EM_PROCESSAMENTO (atual: " + status + ")");
        }
        this.status = StatusPedidoCanal.FALHA;
        this.erroProcessamento = erro;
        this.processadoEm = agora;
    }

    public void cancelar(Instant agora) {
        if (status == StatusPedidoCanal.CONCLUIDO || status == StatusPedidoCanal.CANCELADO) {
            throw new BusinessRuleException("cancelar não permitido a partir de " + status);
        }
        this.status = StatusPedidoCanal.CANCELADO;
        this.canceladoEm = agora;
    }

    private static String exigir(String v, String campo) {
        if (v == null || v.isBlank()) {
            throw new ValidationException(campo + " é obrigatório");
        }
        return v;
    }

    private static BigDecimal exigirNaoNegativo(BigDecimal v, String campo) {
        if (v == null || v.signum() < 0) {
            throw new ValidationException(campo + " não pode ser negativo");
        }
        return v;
    }

    public PedidoCanalId id() { return id; }
    public CanalTipo canalTipo() { return canalTipo; }
    public String pedidoExternoId() { return pedidoExternoId; }
    public Optional<String> displayIdOpt() { return Optional.ofNullable(displayId); }
    public UUID filialId() { return filialId; }
    public CredencialCanalId credencialId() { return credencialId; }
    public StatusPedidoCanal status() { return status; }
    public BigDecimal valorTotal() { return valorTotal; }
    public BigDecimal taxaEntrega() { return taxaEntrega; }
    public BigDecimal taxaServico() { return taxaServico; }
    public BigDecimal valorLiquido() { return valorLiquido; }
    public String moeda() { return moeda; }
    public Optional<String> clienteNomeOpt() { return Optional.ofNullable(clienteNome); }
    public Optional<String> clienteTelefoneOpt() { return Optional.ofNullable(clienteTelefone); }
    public List<ItemPedidoCanal> itens() { return Collections.unmodifiableList(itens); }
    public Optional<UUID> movimentacaoIdOpt() { return Optional.ofNullable(movimentacaoId); }
    public Optional<String> erroProcessamentoOpt() { return Optional.ofNullable(erroProcessamento); }
    public Instant recebidoEm() { return recebidoEm; }
    public Optional<Instant> processadoEmOpt() { return Optional.ofNullable(processadoEm); }
    public Optional<Instant> concluidoEmOpt() { return Optional.ofNullable(concluidoEm); }
    public Optional<Instant> canceladoEmOpt() { return Optional.ofNullable(canceladoEm); }
}
