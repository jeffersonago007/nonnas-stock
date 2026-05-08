package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Categoria de insumo, com hierarquia opcional (categoria pai). Usada para
 * agrupar insumos (ex.: "Carnes", "Laticínios", "Massas") e oferecer escopo
 * de alertas e relatórios.
 */
public final class CategoriaInsumo {

    private final CategoriaInsumoId id;
    private CategoriaInsumoId categoriaPaiId;
    private String nome;
    private boolean ativa;
    private final Instant createdAt;
    private Instant updatedAt;

    public CategoriaInsumo(CategoriaInsumoId id, CategoriaInsumoId categoriaPaiId, String nome,
                           boolean ativa, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.categoriaPaiId = categoriaPaiId;
        this.nome = validarNome(nome);
        this.ativa = ativa;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static CategoriaInsumo nova(String nome, CategoriaInsumoId paiId, Instant agora) {
        return new CategoriaInsumo(CategoriaInsumoId.generate(), paiId, nome, true, agora, agora);
    }

    public void renomear(String novoNome, Instant agora) {
        this.nome = validarNome(novoNome);
        this.updatedAt = agora;
    }

    public void desativar(Instant agora) { this.ativa = false; this.updatedAt = agora; }
    public void ativar(Instant agora) { this.ativa = true; this.updatedAt = agora; }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidationException("Nome da categoria é obrigatório");
        }
        String trimmed = nome.trim();
        if (trimmed.length() > 255) {
            throw new ValidationException("Nome da categoria não pode exceder 255 caracteres");
        }
        return trimmed;
    }

    public CategoriaInsumoId id() { return id; }
    public Optional<CategoriaInsumoId> categoriaPaiId() { return Optional.ofNullable(categoriaPaiId); }
    public String nome() { return nome; }
    public boolean ativa() { return ativa; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
