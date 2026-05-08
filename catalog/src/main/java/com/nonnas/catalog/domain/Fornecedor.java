package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;

public final class Fornecedor {

    private final FornecedorId id;
    private String razaoSocial;
    private final Cnpj cnpj;
    private boolean ativo;
    private final Instant createdAt;
    private Instant updatedAt;

    public Fornecedor(FornecedorId id, String razaoSocial, Cnpj cnpj, boolean ativo,
                      Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.razaoSocial = validarRazao(razaoSocial);
        this.cnpj = Objects.requireNonNull(cnpj);
        this.ativo = ativo;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Fornecedor novo(String razaoSocial, Cnpj cnpj, Instant agora) {
        return new Fornecedor(FornecedorId.generate(), razaoSocial, cnpj, true, agora, agora);
    }

    public void renomear(String nova, Instant agora) {
        this.razaoSocial = validarRazao(nova);
        this.updatedAt = agora;
    }

    public void desativar(Instant agora) { this.ativo = false; this.updatedAt = agora; }
    public void ativar(Instant agora) { this.ativo = true; this.updatedAt = agora; }

    private static String validarRazao(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Razão social é obrigatória");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 255) {
            throw new ValidationException("Razão social não pode exceder 255 caracteres");
        }
        return trimmed;
    }

    public FornecedorId id() { return id; }
    public String razaoSocial() { return razaoSocial; }
    public Cnpj cnpj() { return cnpj; }
    public boolean ativo() { return ativo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
