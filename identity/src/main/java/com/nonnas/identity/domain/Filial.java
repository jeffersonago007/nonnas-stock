package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class Filial {

    private final FilialId id;
    private final EmpresaId empresaId;
    private String nome;
    private final Cnpj cnpj;
    private String endereco;
    private boolean ativa;
    private final Instant createdAt;
    private Instant updatedAt;

    public Filial(FilialId id, EmpresaId empresaId, String nome, Cnpj cnpj,
                  String endereco, boolean ativa,
                  Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
        this.nome = validarNome(nome);
        this.cnpj = Objects.requireNonNull(cnpj, "cnpj");
        this.endereco = endereco;
        this.ativa = ativa;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Filial nova(EmpresaId empresaId, String nome, Cnpj cnpj,
                              String endereco, Instant agora) {
        return new Filial(FilialId.generate(), empresaId, nome, cnpj, endereco, true, agora, agora);
    }

    public void renomear(String novoNome, Instant agora) {
        this.nome = validarNome(novoNome);
        this.updatedAt = agora;
    }

    public void atualizarEndereco(String novoEndereco, Instant agora) {
        this.endereco = novoEndereco;
        this.updatedAt = agora;
    }

    public void desativar(Instant agora) {
        this.ativa = false;
        this.updatedAt = agora;
    }

    public void ativar(Instant agora) {
        this.ativa = true;
        this.updatedAt = agora;
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidationException("Nome da filial é obrigatório");
        }
        String trimmed = nome.trim();
        if (trimmed.length() > 255) {
            throw new ValidationException("Nome da filial não pode exceder 255 caracteres");
        }
        return trimmed;
    }

    public FilialId id() { return id; }
    public EmpresaId empresaId() { return empresaId; }
    public String nome() { return nome; }
    public Cnpj cnpj() { return cnpj; }
    public Optional<String> endereco() { return Optional.ofNullable(endereco); }
    public boolean ativa() { return ativa; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
