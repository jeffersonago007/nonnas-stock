package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ProdutoVendavel {

    private final ProdutoVendavelId id;
    private String codigo;
    private String nome;
    private String categoria;
    private final TipoProdutoVendavel tipo;
    private final UUID insumoRevendaId;
    private boolean ativo;
    private final Instant createdAt;
    private Instant updatedAt;

    public ProdutoVendavel(ProdutoVendavelId id, String codigo, String nome, String categoria,
                           TipoProdutoVendavel tipo, UUID insumoRevendaId,
                           boolean ativo, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.codigo = validarCodigo(codigo);
        this.nome = validarNome(nome);
        this.categoria = validarCategoria(categoria);
        this.tipo = Objects.requireNonNull(tipo, "Tipo do produto vendável é obrigatório");
        this.insumoRevendaId = validarCoerenciaTipoInsumo(tipo, insumoRevendaId);
        this.ativo = ativo;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static ProdutoVendavel novoFabricado(String codigo, String nome, String categoria, Instant agora) {
        return new ProdutoVendavel(ProdutoVendavelId.generate(), codigo, nome, categoria,
                TipoProdutoVendavel.FABRICADO, null, true, agora, agora);
    }

    /** Atalho histórico — equivalente a {@link #novoFabricado}. */
    public static ProdutoVendavel novo(String codigo, String nome, String categoria, Instant agora) {
        return novoFabricado(codigo, nome, categoria, agora);
    }

    public static ProdutoVendavel novoRevenda(String codigo, String nome, String categoria,
                                              UUID insumoRevendaId, Instant agora) {
        return new ProdutoVendavel(ProdutoVendavelId.generate(), codigo, nome, categoria,
                TipoProdutoVendavel.REVENDA, insumoRevendaId, true, agora, agora);
    }

    public void renomear(String novoNome, Instant agora) {
        this.nome = validarNome(novoNome);
        this.updatedAt = agora;
    }

    public void recategorizar(String novaCategoria, Instant agora) {
        this.categoria = validarCategoria(novaCategoria);
        this.updatedAt = agora;
    }

    public void ativar(Instant agora) { this.ativo = true; this.updatedAt = agora; }
    public void desativar(Instant agora) { this.ativo = false; this.updatedAt = agora; }

    private static UUID validarCoerenciaTipoInsumo(TipoProdutoVendavel tipo, UUID insumoRevendaId) {
        if (tipo == TipoProdutoVendavel.REVENDA && insumoRevendaId == null) {
            throw new ValidationException("Produto de revenda exige insumo vinculado");
        }
        if (tipo == TipoProdutoVendavel.FABRICADO && insumoRevendaId != null) {
            throw new ValidationException("Produto fabricado não pode ter insumo de revenda");
        }
        return insumoRevendaId;
    }

    private static String validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ValidationException("Código do produto vendável é obrigatório");
        }
        String c = codigo.trim();
        if (c.length() > 50) {
            throw new ValidationException("Código do produto vendável não pode exceder 50 caracteres");
        }
        return c;
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidationException("Nome do produto vendável é obrigatório");
        }
        String n = nome.trim();
        if (n.length() > 150) {
            throw new ValidationException("Nome do produto vendável não pode exceder 150 caracteres");
        }
        return n.toUpperCase(Locale.ROOT);
    }

    private static String validarCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new ValidationException("Categoria do produto vendável é obrigatória");
        }
        String c = categoria.trim();
        if (c.length() > 50) {
            throw new ValidationException("Categoria do produto vendável não pode exceder 50 caracteres");
        }
        return c;
    }

    public ProdutoVendavelId id() { return id; }
    public String codigo() { return codigo; }
    public String nome() { return nome; }
    public String categoria() { return categoria; }
    public TipoProdutoVendavel tipo() { return tipo; }
    public Optional<UUID> insumoRevendaIdOpt() { return Optional.ofNullable(insumoRevendaId); }
    public boolean ativo() { return ativo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
