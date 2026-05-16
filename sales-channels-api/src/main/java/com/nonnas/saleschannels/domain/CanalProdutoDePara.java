package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Mapeamento {@code (canal, externalCode) → ProdutoVendavel}. Pode ser
 * global (sem filial) ou específico por filial — o lookup tenta primeiro
 * o específico, depois cai para o global.
 *
 * <p>Sem FK física para {@code produtos_vendaveis} (cross-context recipes,
 * padrão T03/ADR 0010). Validação de existência do produto fica no use
 * case que faz a vinculação.
 */
public final class CanalProdutoDePara {

    private final CanalProdutoDeParaId id;
    private final CanalTipo canalTipo;
    private final String externalCode;
    private final UUID filialId;
    private UUID produtoVendavelId;
    private String observacao;
    private final Instant createdAt;
    private Instant updatedAt;

    public CanalProdutoDePara(CanalProdutoDeParaId id, CanalTipo canalTipo,
                               String externalCode, UUID filialId,
                               UUID produtoVendavelId, String observacao,
                               Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.canalTipo = Objects.requireNonNull(canalTipo);
        this.externalCode = exigir(externalCode, "externalCode");
        this.filialId = filialId;
        this.produtoVendavelId = Objects.requireNonNull(produtoVendavelId, "produtoVendavelId");
        this.observacao = observacao;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static CanalProdutoDePara novo(CanalTipo canalTipo, String externalCode,
                                          UUID filialId, UUID produtoVendavelId,
                                          String observacao, Instant agora) {
        return new CanalProdutoDePara(CanalProdutoDeParaId.generate(),
                canalTipo, externalCode, filialId, produtoVendavelId,
                observacao, agora, agora);
    }

    public void redirecionarProduto(UUID novoProdutoVendavelId, Instant agora) {
        this.produtoVendavelId = Objects.requireNonNull(novoProdutoVendavelId);
        this.updatedAt = agora;
    }

    public void atualizarObservacao(String novaObservacao, Instant agora) {
        this.observacao = novaObservacao;
        this.updatedAt = agora;
    }

    private static String exigir(String v, String campo) {
        if (v == null || v.isBlank()) {
            throw new ValidationException(campo + " é obrigatório");
        }
        return v;
    }

    public CanalProdutoDeParaId id() { return id; }
    public CanalTipo canalTipo() { return canalTipo; }
    public String externalCode() { return externalCode; }
    public Optional<UUID> filialIdOpt() { return Optional.ofNullable(filialId); }
    public UUID produtoVendavelId() { return produtoVendavelId; }
    public Optional<String> observacaoOpt() { return Optional.ofNullable(observacao); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
