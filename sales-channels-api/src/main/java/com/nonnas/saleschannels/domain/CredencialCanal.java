package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Credencial de um canal para uma filial específica.
 *
 * <p>O segredo é armazenado <em>cifrado</em> ({@link #clientSecretCifrado}) —
 * a cifragem AES-256-GCM é responsabilidade do caller (CryptoService de T16).
 * O domínio nunca vê o segredo em claro.
 *
 * <p>A unicidade "uma credencial ativa por (canal, filial)" é garantida no
 * schema via partial unique index. Para rotacionar uma credencial: cria-se
 * uma nova ativa e desativa-se a anterior na mesma transação.
 */
public final class CredencialCanal {

    private final CredencialCanalId id;
    private final CanalTipo canalTipo;
    private final UUID filialId;
    private final String merchantExternoId;
    private final String clientId;
    private String clientSecretCifrado;
    private String baseUrl;
    private boolean ativa;
    private String observacao;
    private final Instant createdAt;
    private Instant updatedAt;

    public CredencialCanal(CredencialCanalId id, CanalTipo canalTipo, UUID filialId,
                           String merchantExternoId, String clientId,
                           String clientSecretCifrado, String baseUrl, boolean ativa,
                           String observacao, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.canalTipo = Objects.requireNonNull(canalTipo);
        this.filialId = Objects.requireNonNull(filialId);
        this.merchantExternoId = exigir(merchantExternoId, "merchantExternoId");
        this.clientId = exigir(clientId, "clientId");
        this.clientSecretCifrado = exigir(clientSecretCifrado, "clientSecretCifrado");
        this.baseUrl = baseUrl;
        this.ativa = ativa;
        this.observacao = observacao;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static CredencialCanal nova(CanalTipo canalTipo, UUID filialId,
                                       String merchantExternoId, String clientId,
                                       String clientSecretCifrado, String baseUrl,
                                       String observacao, Instant agora) {
        return new CredencialCanal(CredencialCanalId.generate(), canalTipo, filialId,
                merchantExternoId, clientId, clientSecretCifrado, baseUrl, true,
                observacao, agora, agora);
    }

    public void rotacionarSegredo(String novoSegredoCifrado, Instant agora) {
        this.clientSecretCifrado = exigir(novoSegredoCifrado, "clientSecretCifrado");
        this.updatedAt = agora;
    }

    public void atualizarBaseUrl(String novaBaseUrl, Instant agora) {
        this.baseUrl = novaBaseUrl;
        this.updatedAt = agora;
    }

    public void atualizarObservacao(String novaObservacao, Instant agora) {
        this.observacao = novaObservacao;
        this.updatedAt = agora;
    }

    public void ativar(Instant agora) { this.ativa = true; this.updatedAt = agora; }
    public void desativar(Instant agora) { this.ativa = false; this.updatedAt = agora; }

    private static String exigir(String v, String campo) {
        if (v == null || v.isBlank()) {
            throw new ValidationException(campo + " é obrigatório");
        }
        return v;
    }

    public CredencialCanalId id() { return id; }
    public CanalTipo canalTipo() { return canalTipo; }
    public UUID filialId() { return filialId; }
    public String merchantExternoId() { return merchantExternoId; }
    public String clientId() { return clientId; }
    public String clientSecretCifrado() { return clientSecretCifrado; }
    public Optional<String> baseUrlOpt() { return Optional.ofNullable(baseUrl); }
    public boolean ativa() { return ativa; }
    public Optional<String> observacaoOpt() { return Optional.ofNullable(observacao); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
