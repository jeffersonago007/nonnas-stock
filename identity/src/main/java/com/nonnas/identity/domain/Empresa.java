package com.nonnas.identity.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Empresa do grupo Nonnas Paola. Cada empresa pode ter múltiplas filiais.
 * Entidade do domínio: independente de JPA, instanciável em testes sem
 * container.
 */
public final class Empresa {

    private final EmpresaId id;
    private RazaoSocial razaoSocial;
    private final Cnpj cnpj;
    private boolean ativa;
    private final Instant createdAt;
    private Instant updatedAt;

    public Empresa(EmpresaId id, RazaoSocial razaoSocial, Cnpj cnpj, boolean ativa,
                   Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.razaoSocial = Objects.requireNonNull(razaoSocial, "razaoSocial");
        this.cnpj = Objects.requireNonNull(cnpj, "cnpj");
        this.ativa = ativa;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Empresa nova(RazaoSocial razaoSocial, Cnpj cnpj, Instant agora) {
        return new Empresa(EmpresaId.generate(), razaoSocial, cnpj, true, agora, agora);
    }

    public void renomear(RazaoSocial nova, Instant agora) {
        this.razaoSocial = Objects.requireNonNull(nova);
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

    public EmpresaId id() { return id; }
    public RazaoSocial razaoSocial() { return razaoSocial; }
    public Cnpj cnpj() { return cnpj; }
    public boolean ativa() { return ativa; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
