package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Ficha técnica versionada de um produto vendável. Edição não muta a
 * receita: {@link #editar(List, Instant)} desativa esta versão e devolve
 * uma nova ficha (versao+1) ativa. Ambas devem ser persistidas pelo use
 * case na mesma transação.
 *
 * <p>Itens são imutáveis (defensive copy), garantindo snapshot quando uma
 * venda referencia esta ficha.
 */
public final class FichaTecnica {

    private final FichaTecnicaId id;
    private final ProdutoVendavelId produtoVendavelId;
    private final int versao;
    private final Instant vigenteDesde;
    private Instant vigenteAte;
    private boolean ativa;
    private final List<ItemFichaTecnica> itens;
    private final Instant createdAt;
    private Instant updatedAt;

    public FichaTecnica(FichaTecnicaId id, ProdutoVendavelId produtoVendavelId, int versao,
                        Instant vigenteDesde, Instant vigenteAte, boolean ativa,
                        List<ItemFichaTecnica> itens, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.produtoVendavelId = Objects.requireNonNull(produtoVendavelId);
        if (versao <= 0) {
            throw new ValidationException("Versão da ficha técnica deve ser positiva");
        }
        this.versao = versao;
        this.vigenteDesde = Objects.requireNonNull(vigenteDesde);
        if (vigenteAte != null && vigenteAte.isBefore(vigenteDesde)) {
            throw new ValidationException("Vigente até não pode ser anterior a vigente desde");
        }
        this.vigenteAte = vigenteAte;
        this.ativa = ativa;
        this.itens = new ArrayList<>(validarItens(itens));
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static FichaTecnica nova(ProdutoVendavelId produtoVendavelId, int versao,
                                    List<ItemFichaTecnica> itens, Instant agora) {
        return new FichaTecnica(FichaTecnicaId.generate(), produtoVendavelId, versao,
                agora, null, true, itens, agora, agora);
    }

    /**
     * Encerra esta versão e devolve uma nova ficha (versao+1) ativa com
     * os itens fornecidos. Esta instância passa a ter
     * {@code vigenteAte=agora} e {@code ativa=false}.
     */
    public FichaTecnica editar(List<ItemFichaTecnica> novosItens, Instant agora) {
        if (!ativa) {
            throw new ValidationException("Não é possível editar uma ficha técnica inativa");
        }
        this.ativa = false;
        this.vigenteAte = agora;
        this.updatedAt = agora;
        return FichaTecnica.nova(this.produtoVendavelId, this.versao + 1, novosItens, agora);
    }

    private static List<ItemFichaTecnica> validarItens(List<ItemFichaTecnica> itens) {
        Objects.requireNonNull(itens, "itens");
        if (itens.isEmpty()) {
            throw new ValidationException("Ficha técnica deve ter ao menos um item");
        }
        var insumosVistos = new HashSet<UUID>();
        for (var item : itens) {
            if (!insumosVistos.add(item.insumoId())) {
                throw new ValidationException("Insumo duplicado na ficha técnica: " + item.insumoId());
            }
        }
        return itens;
    }

    public FichaTecnicaId id() { return id; }
    public ProdutoVendavelId produtoVendavelId() { return produtoVendavelId; }
    public int versao() { return versao; }
    public Instant vigenteDesde() { return vigenteDesde; }
    public Optional<Instant> vigenteAteOpt() { return Optional.ofNullable(vigenteAte); }
    public boolean ativa() { return ativa; }
    public List<ItemFichaTecnica> itens() { return Collections.unmodifiableList(itens); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
