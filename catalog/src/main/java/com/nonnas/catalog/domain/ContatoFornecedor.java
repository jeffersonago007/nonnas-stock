package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Contato opcional de um fornecedor (vendedor, financeiro, suporte etc.).
 * Pelo menos um dos três campos {@code nome}, {@code email}, {@code telefone}
 * deve estar preenchido — a entrada em branco não tem utilidade prática.
 */
public final class ContatoFornecedor {

    private final UUID id;
    private final String nome;
    private final String email;
    private final String telefone;

    public ContatoFornecedor(UUID id, String nome, String email, String telefone) {
        this.id = Objects.requireNonNull(id, "id");
        this.nome = trimOrNull(nome);
        this.email = trimOrNull(email);
        this.telefone = trimOrNull(telefone);
        if (this.nome == null && this.email == null && this.telefone == null) {
            throw new ValidationException("Contato deve ter ao menos nome, e-mail ou telefone");
        }
    }

    public static ContatoFornecedor novo(String nome, String email, String telefone) {
        return new ContatoFornecedor(UUID.randomUUID(), nome, email, telefone);
    }

    private static String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public UUID id() { return id; }
    public Optional<String> nomeOpt() { return Optional.ofNullable(nome); }
    public Optional<String> emailOpt() { return Optional.ofNullable(email); }
    public Optional<String> telefoneOpt() { return Optional.ofNullable(telefone); }
}
