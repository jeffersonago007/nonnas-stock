package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class Fornecedor {

    private final FornecedorId id;
    private String razaoSocial;
    private final Cnpj cnpj;
    private boolean ativo;
    private final List<ContatoFornecedor> contatos;
    private final Instant createdAt;
    private Instant updatedAt;

    public Fornecedor(FornecedorId id, String razaoSocial, Cnpj cnpj, boolean ativo,
                      List<ContatoFornecedor> contatos,
                      Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.razaoSocial = validarRazao(razaoSocial);
        this.cnpj = Objects.requireNonNull(cnpj);
        this.ativo = ativo;
        this.contatos = new ArrayList<>(contatos == null ? List.of() : contatos);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Fornecedor novo(String razaoSocial, Cnpj cnpj, Instant agora) {
        return new Fornecedor(FornecedorId.generate(), razaoSocial, cnpj, true,
                List.of(), agora, agora);
    }

    public static Fornecedor novo(String razaoSocial, Cnpj cnpj,
                                  List<ContatoFornecedor> contatos, Instant agora) {
        return new Fornecedor(FornecedorId.generate(), razaoSocial, cnpj, true,
                contatos, agora, agora);
    }

    public void renomear(String nova, Instant agora) {
        this.razaoSocial = validarRazao(nova);
        this.updatedAt = agora;
    }

    public void desativar(Instant agora) { this.ativo = false; this.updatedAt = agora; }
    public void ativar(Instant agora) { this.ativo = true; this.updatedAt = agora; }

    /**
     * Substitui inteira a lista de contatos. Não há sincronização item-a-item
     * — frontend manda o estado final desejado e este método apaga os antigos
     * via {@code orphanRemoval} no JPA mapping.
     */
    public void definirContatos(List<ContatoFornecedor> novosContatos, Instant agora) {
        this.contatos.clear();
        if (novosContatos != null) {
            this.contatos.addAll(novosContatos);
        }
        this.updatedAt = agora;
    }

    /**
     * Razão social sempre normalizada em UPPERCASE — convenção de cadastro
     * para alinhar com dados que chegam via NF-e (que vêm em maiúsculas) e
     * evitar duplicidade aparente por diferença de capitalização.
     */
    private static String validarRazao(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Razão social é obrigatória");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 255) {
            throw new ValidationException("Razão social não pode exceder 255 caracteres");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public FornecedorId id() { return id; }
    public String razaoSocial() { return razaoSocial; }
    public Cnpj cnpj() { return cnpj; }
    public boolean ativo() { return ativo; }
    public List<ContatoFornecedor> contatos() { return Collections.unmodifiableList(contatos); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
