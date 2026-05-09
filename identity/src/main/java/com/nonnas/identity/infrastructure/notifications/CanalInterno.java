package com.nonnas.identity.infrastructure.notifications;

import com.nonnas.identity.application.notifications.CanalNotificacao;
import com.nonnas.identity.application.notifications.Notificacao;
import com.nonnas.identity.application.notifications.NovaNotificacao;
import com.nonnas.identity.application.ports.NotificacaoRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Implementação default de {@link CanalNotificacao} — grava na tabela
 * {@code notificacoes_usuario}. Sempre disponível.
 *
 * <p>Implementações futuras (CanalEmail, CanalWhatsApp) serão @Component
 * adicionais e a {@code NotificacaoInternaService} as despacha em paralelo
 * conforme {@code canaisDestino} de cada notificação.
 */
@Component
public class CanalInterno implements CanalNotificacao {

    public static final String NOME = "INTERNO";

    private final NotificacaoRepository repository;
    private final Clock clock;

    public CanalInterno(NotificacaoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public void despachar(NovaNotificacao nova) {
        repository.save(new Notificacao(
                UUID.randomUUID(),
                nova.usuarioId(),
                nova.tipo(),
                nova.prioridade(),
                nova.titulo(),
                nova.mensagem(),
                nova.linkAcao(),
                nova.metadataJson(),
                nova.canaisDestino(),
                clock.instant(),
                null,
                null));
    }
}
