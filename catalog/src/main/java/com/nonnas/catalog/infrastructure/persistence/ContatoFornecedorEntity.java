package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.web.crypto.CamposSensiveisConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fornecedores_contatos")
public class ContatoFornecedorEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    // PII LGPD — cifrado em repouso (T-QOL-03). length expandido pra ciphertext.
    @Convert(converter = CamposSensiveisConverter.class)
    @Column(length = 500) private String nome;
    @Convert(converter = CamposSensiveisConverter.class)
    @Column(length = 500) private String email;
    @Convert(converter = CamposSensiveisConverter.class)
    @Column(length = 500) private String telefone;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public ContatoFornecedorEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public String getNome() { return nome; } public void setNome(String v) { this.nome = v; }
    public String getEmail() { return email; } public void setEmail(String v) { this.email = v; }
    public String getTelefone() { return telefone; } public void setTelefone(String v) { this.telefone = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
