package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Usuário do sistema. Carrega o estado de proteção contra brute force
 * (tentativas falhas, bloqueio temporário e travamento) diretamente como
 * atributos da entidade — opção pragmática para evitar JOIN extra em
 * todo login. Refatorável em tabela dedicada se a contenção crescer.
 *
 * <p>Política de bloqueio (master doc seção 13.3):
 * <ul>
 *   <li>3 falhas consecutivas → bloqueio de 15 minutos</li>
 *   <li>5 falhas consecutivas → bloqueio de 1 hora</li>
 *   <li>10 falhas consecutivas → conta travada (libera só por ADMIN)</li>
 * </ul>
 */
public final class Usuario {

    public static final int LIMIAR_BLOQUEIO_15MIN = 3;
    public static final int LIMIAR_BLOQUEIO_1H = 5;
    public static final int LIMIAR_TRAVAMENTO = 10;

    private final UsuarioId id;
    private FilialId filialId;
    private String nome;
    private Email email;
    private SenhaHash senhaHash;
    private Perfil perfil;
    private boolean ativo;
    private int tentativasFalhas;
    private Instant bloqueadoAte;
    private boolean travada;
    private final Instant createdAt;
    private Instant updatedAt;

    public Usuario(UsuarioId id, FilialId filialId, String nome, Email email,
                   SenhaHash senhaHash, Perfil perfil, boolean ativo,
                   int tentativasFalhas, Instant bloqueadoAte, boolean travada,
                   Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.filialId = filialId;
        this.nome = validarNome(nome);
        this.email = Objects.requireNonNull(email, "email");
        this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash");
        this.perfil = Objects.requireNonNull(perfil, "perfil");
        this.ativo = ativo;
        if (tentativasFalhas < 0) {
            throw new ValidationException("tentativasFalhas não pode ser negativa");
        }
        this.tentativasFalhas = tentativasFalhas;
        this.bloqueadoAte = bloqueadoAte;
        this.travada = travada;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Usuario novo(FilialId filialId, String nome, Email email,
                               SenhaHash senhaHash, Perfil perfil, Instant agora) {
        return new Usuario(UsuarioId.generate(), filialId, nome, email, senhaHash,
                perfil, true, 0, null, false, agora, agora);
    }

    public boolean estaBloqueado(Instant agora) {
        return travada || (bloqueadoAte != null && agora.isBefore(bloqueadoAte));
    }

    public boolean podeLogar(Instant agora) {
        return ativo && !estaBloqueado(agora);
    }

    /**
     * Aplica política de bloqueio progressivo. Não persiste — chamada deve
     * ser seguida de save no repositório.
     */
    public void registrarLoginFalho(Instant agora) {
        Objects.requireNonNull(agora);
        tentativasFalhas++;
        if (tentativasFalhas >= LIMIAR_TRAVAMENTO) {
            travada = true;
            bloqueadoAte = null;
        } else if (tentativasFalhas >= LIMIAR_BLOQUEIO_1H) {
            bloqueadoAte = agora.plus(Duration.ofHours(1));
        } else if (tentativasFalhas >= LIMIAR_BLOQUEIO_15MIN) {
            bloqueadoAte = agora.plus(Duration.ofMinutes(15));
        }
        this.updatedAt = agora;
    }

    public void registrarLoginSucesso(Instant agora) {
        Objects.requireNonNull(agora);
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        // travada permanece — só ADMIN libera
        this.updatedAt = agora;
    }

    public void liberar(Instant agora) {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.travada = false;
        this.updatedAt = agora;
    }

    public void alterarSenha(SenhaHash novaSenha, Instant agora) {
        this.senhaHash = Objects.requireNonNull(novaSenha);
        this.updatedAt = agora;
    }

    public void alterarPerfil(Perfil novoPerfil, Instant agora) {
        this.perfil = Objects.requireNonNull(novoPerfil);
        this.updatedAt = agora;
    }

    public void renomear(String novoNome, Instant agora) {
        this.nome = validarNome(novoNome);
        this.updatedAt = agora;
    }

    /**
     * Troca de e-mail — usado em fluxos LGPD (correção do Art. 18 III e
     * anonimização do Art. 18 VI). Mantém o id, então integridade referencial
     * (movimentações, transferências, audit log) permanece.
     */
    public void alterarEmail(Email novoEmail, Instant agora) {
        this.email = Objects.requireNonNull(novoEmail, "email");
        this.updatedAt = agora;
    }

    public void moverPara(FilialId novaFilialId, Instant agora) {
        this.filialId = novaFilialId;
        this.updatedAt = agora;
    }

    public void desativar(Instant agora) { this.ativo = false; this.updatedAt = agora; }
    public void ativar(Instant agora) { this.ativo = true; this.updatedAt = agora; }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidationException("Nome do usuário é obrigatório");
        }
        String trimmed = nome.trim();
        if (trimmed.length() > 255) {
            throw new ValidationException("Nome do usuário não pode exceder 255 caracteres");
        }
        return trimmed;
    }

    public UsuarioId id() { return id; }
    public Optional<FilialId> filialId() { return Optional.ofNullable(filialId); }
    public String nome() { return nome; }
    public Email email() { return email; }
    public SenhaHash senhaHash() { return senhaHash; }
    public Perfil perfil() { return perfil; }
    public boolean ativo() { return ativo; }
    public int tentativasFalhas() { return tentativasFalhas; }
    public Optional<Instant> bloqueadoAte() { return Optional.ofNullable(bloqueadoAte); }
    public boolean travada() { return travada; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
