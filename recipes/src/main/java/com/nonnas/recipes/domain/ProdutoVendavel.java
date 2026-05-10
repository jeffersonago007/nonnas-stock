package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Produto vendável do cardápio (pizza, churrasco, prato, bebida etc.).
 * Aponta para a ficha técnica vigente via {@link FichaTecnica#produtoVendavelId()}.
 */
public final class ProdutoVendavel {

    private final ProdutoVendavelId id;
    private String codigo;
    private String nome;
    private String categoria;
    private boolean ativo;
    private final Instant createdAt;
    private Instant updatedAt;

    public ProdutoVendavel(ProdutoVendavelId id, String codigo, String nome, String categoria,
                           boolean ativo, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.codigo = validarCodigo(codigo);
        this.nome = validarNome(nome);
        this.categoria = validarCategoria(categoria);
        this.ativo = ativo;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static ProdutoVendavel novo(String codigo, String nome, String categoria, Instant agora) {
        return new ProdutoVendavel(ProdutoVendavelId.generate(), codigo, nome, categoria, true, agora, agora);
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

    /**
     * Nome sempre normalizado em UPPERCASE — convenção de cadastro para
     * alinhar com dados que chegam via NF-e (que vêm em maiúsculas) e
     * evitar duplicidade aparente por diferença de capitalização.
     */
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
    public boolean ativo() { return ativo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
