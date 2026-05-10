package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class Insumo {

    private final InsumoId id;
    private String codigo;
    private String nome;
    private CategoriaInsumoId categoriaId;
    private UnidadeMedidaId unidadeBaseId;
    private boolean controlaLote;
    private boolean controlaValidade;
    private boolean ativo;
    private final Instant createdAt;
    private Instant updatedAt;

    public Insumo(InsumoId id, String codigo, String nome, CategoriaInsumoId categoriaId,
                  UnidadeMedidaId unidadeBaseId, boolean controlaLote, boolean controlaValidade,
                  boolean ativo, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.codigo = validarCodigo(codigo);
        this.nome = validarNome(nome);
        this.categoriaId = Objects.requireNonNull(categoriaId, "categoriaId");
        this.unidadeBaseId = Objects.requireNonNull(unidadeBaseId, "unidadeBaseId");
        this.controlaLote = controlaLote;
        this.controlaValidade = controlaValidade;
        this.ativo = ativo;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Insumo novo(String codigo, String nome, CategoriaInsumoId categoriaId,
                              UnidadeMedidaId unidadeBaseId, boolean controlaLote,
                              boolean controlaValidade, Instant agora) {
        return new Insumo(InsumoId.generate(), codigo, nome, categoriaId, unidadeBaseId,
                controlaLote, controlaValidade, true, agora, agora);
    }

    public void renomear(String novoNome, Instant agora) {
        this.nome = validarNome(novoNome);
        this.updatedAt = agora;
    }

    /**
     * Permite reclassificar o insumo. Não há regra de negócio impedindo;
     * alertas/relatórios atrelados ao escopo CATEGORIA passam a aplicar à
     * nova categoria após o save.
     */
    public void mudarCategoria(CategoriaInsumoId novaCategoriaId, Instant agora) {
        this.categoriaId = Objects.requireNonNull(novaCategoriaId, "categoriaId");
        this.updatedAt = agora;
    }

    /**
     * Liga/desliga rastreamento de lote. Não bloqueia se já existem lotes
     * registrados — fica como ressalva operacional (lotes existentes
     * continuam, novas movimentações deixam de exigir lote).
     */
    public void definirControlaLote(boolean controlaLote, Instant agora) {
        this.controlaLote = controlaLote;
        this.updatedAt = agora;
    }

    public void definirControlaValidade(boolean controlaValidade, Instant agora) {
        this.controlaValidade = controlaValidade;
        this.updatedAt = agora;
    }

    public void desativar(Instant agora) { this.ativo = false; this.updatedAt = agora; }
    public void ativar(Instant agora) { this.ativo = true; this.updatedAt = agora; }

    private static String validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ValidationException("Código do insumo é obrigatório");
        }
        String c = codigo.trim();
        if (c.length() > 50) {
            throw new ValidationException("Código do insumo não pode exceder 50 caracteres");
        }
        return c;
    }

    /**
     * Nome sempre normalizado em UPPERCASE — convenção de cadastro para
     * alinhar com dados que chegam via NF-e (que vêm em maiúsculas) e
     * evitar duplicidade aparente por diferença de capitalização.
     */
    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidationException("Nome do insumo é obrigatório");
        }
        String n = nome.trim();
        if (n.length() > 255) {
            throw new ValidationException("Nome do insumo não pode exceder 255 caracteres");
        }
        return n.toUpperCase(Locale.ROOT);
    }

    public InsumoId id() { return id; }
    public String codigo() { return codigo; }
    public String nome() { return nome; }
    public CategoriaInsumoId categoriaId() { return categoriaId; }
    public UnidadeMedidaId unidadeBaseId() { return unidadeBaseId; }
    public boolean controlaLote() { return controlaLote; }
    public boolean controlaValidade() { return controlaValidade; }
    public boolean ativo() { return ativo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
