package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;

public final class UnidadeMedida {

    private final UnidadeMedidaId id;
    private String codigo;
    private String nome;
    private UnidadeMedidaTipo tipo;
    private boolean ativa;
    private final Instant createdAt;
    private Instant updatedAt;

    public UnidadeMedida(UnidadeMedidaId id, String codigo, String nome, UnidadeMedidaTipo tipo,
                         boolean ativa, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.codigo = validarCodigo(codigo);
        this.nome = validarNome(nome);
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.ativa = ativa;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static UnidadeMedida nova(String codigo, String nome, UnidadeMedidaTipo tipo, Instant agora) {
        return new UnidadeMedida(UnidadeMedidaId.generate(), codigo, nome, tipo, true, agora, agora);
    }

    public void desativar(Instant agora) { this.ativa = false; this.updatedAt = agora; }
    public void ativar(Instant agora) { this.ativa = true; this.updatedAt = agora; }

    private static String validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ValidationException("Código da unidade é obrigatório");
        }
        String c = codigo.trim().toUpperCase();
        if (c.length() > 20) {
            throw new ValidationException("Código da unidade não pode exceder 20 caracteres");
        }
        return c;
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidationException("Nome da unidade é obrigatório");
        }
        String trimmed = nome.trim();
        if (trimmed.length() > 100) {
            throw new ValidationException("Nome da unidade não pode exceder 100 caracteres");
        }
        return trimmed;
    }

    public UnidadeMedidaId id() { return id; }
    public String codigo() { return codigo; }
    public String nome() { return nome; }
    public UnidadeMedidaTipo tipo() { return tipo; }
    public boolean ativa() { return ativa; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
