package com.nonnas.identity.application.password;

import com.nonnas.identity.domain.UsuarioId;
import com.nonnas.identity.infrastructure.persistence.HistoricoSenhaEntity;
import com.nonnas.identity.infrastructure.persistence.HistoricoSenhaJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Mantém um sliding window das últimas 5 senhas de cada usuário e rejeita
 * tentativas de reuso. Usado quando o endpoint de troca de senha for
 * implementado em T16 — em T02 fica o serviço pronto e a tabela populada
 * já no momento da criação do usuário (entry inicial = primeira senha).
 */
@Service
public class HistoricoSenhaService {

    public static final int JANELA_HISTORICO = 5;

    private final HistoricoSenhaJpaRepository repository;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public HistoricoSenhaService(HistoricoSenhaJpaRepository repository,
                                 PasswordEncoder encoder,
                                 Clock clock) {
        this.repository = repository;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Transactional
    public void registrarSenha(UsuarioId usuarioId, String senhaHash) {
        HistoricoSenhaEntity entry = new HistoricoSenhaEntity();
        entry.setId(UUID.randomUUID());
        entry.setUsuarioId(usuarioId.value());
        entry.setSenhaHash(senhaHash);
        entry.setCriadaEm(clock.instant());
        repository.save(entry);
    }

    /**
     * @return true se a senha em texto plano coincidir com qualquer um dos
     *         últimos {@value #JANELA_HISTORICO} hashes registrados para o
     *         usuário (ou seja, é reuso e deve ser rejeitada).
     */
    @Transactional(readOnly = true)
    public boolean foiUsadaRecentemente(UsuarioId usuarioId, String senhaPlaintext) {
        List<HistoricoSenhaEntity> recent = repository.findByUsuarioIdOrderByCriadaEmDesc(
                usuarioId.value(), PageRequest.of(0, JANELA_HISTORICO));
        return recent.stream().anyMatch(e -> encoder.matches(senhaPlaintext, e.getSenhaHash()));
    }
}
