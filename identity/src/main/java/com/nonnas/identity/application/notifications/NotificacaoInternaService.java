package com.nonnas.identity.application.notifications;

import com.nonnas.identity.application.ports.NotificacaoRepository;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Coordena criação, leitura, marcação de lida/arquivada de notificações
 * internas. Recebe {@link NovaNotificacao} de event listeners (master doc
 * 15.4: {@code @EventListener} em eventos de domínio críticos).
 *
 * <p>Despacha pra cada {@link CanalNotificacao} listado em
 * {@code canaisDestino}. Hoje só {@code CanalInterno} está registrado;
 * canais futuros (email, WhatsApp) plugam aqui sem mudar consumidores.
 */
@Service
public class NotificacaoInternaService {

    private final NotificacaoRepository repository;
    private final List<CanalNotificacao> canais;
    private final Clock clock;

    public NotificacaoInternaService(NotificacaoRepository repository,
                                     List<CanalNotificacao> canais,
                                     Clock clock) {
        this.repository = repository;
        this.canais = canais;
        this.clock = clock;
    }

    @Transactional
    public void criar(NovaNotificacao nova) {
        for (CanalNotificacao canal : canais) {
            if (nova.canaisDestino().contains(canal.nome())) {
                canal.despachar(nova);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Notificacao> listar(UUID usuarioId, String tipo, boolean incluirArquivadas,
                                    boolean somenteNaoLidas, int page, int size) {
        return repository.findFiltered(usuarioId, tipo, incluirArquivadas, somenteNaoLidas, page, size);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(UUID usuarioId) {
        return repository.countNaoLidas(usuarioId);
    }

    @Transactional
    public void marcarLida(UUID notificacaoId, UUID usuarioRequisitante) {
        Notificacao n = repository.findById(notificacaoId)
                .orElseThrow(() -> new NotFoundException("Notificação", notificacaoId));
        validarOwnership(n, usuarioRequisitante);
        repository.marcarLida(notificacaoId, clock.instant());
    }

    @Transactional
    public int marcarTodasLidas(UUID usuarioId) {
        return repository.marcarTodasLidas(usuarioId, clock.instant());
    }

    @Transactional
    public void arquivar(UUID notificacaoId, UUID usuarioRequisitante) {
        Notificacao n = repository.findById(notificacaoId)
                .orElseThrow(() -> new NotFoundException("Notificação", notificacaoId));
        validarOwnership(n, usuarioRequisitante);
        repository.arquivar(notificacaoId, clock.instant());
    }

    private void validarOwnership(Notificacao n, UUID usuarioRequisitante) {
        if (!n.usuarioId().equals(usuarioRequisitante)) {
            throw new NotFoundException("Notificação", n.id());
            // Mascarar como 404 evita enumeração de IDs alheios.
        }
    }
}
