package com.nonnas.identity.infrastructure.notifications;

import com.nonnas.identity.application.notifications.Notificacao;
import com.nonnas.identity.application.notifications.NotificacaoInternaService;
import com.nonnas.identity.application.notifications.NovaNotificacao;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.sharedkernel.events.AlertaDisparadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Materializa {@link AlertaDisparadoEvent} em notificações internas para
 * os usuários relevantes da filial alvo.
 *
 * <p>Para MVP, distribuímos para ADMIN (todos) + GERENTE da filial. Em
 * produção isso vai virar uma query de subscription ({@code preferencias_notificacao})
 * na onda 1.2; por enquanto o critério é estrutural.
 *
 * <p>{@code @TransactionalEventListener AFTER_COMMIT} evita criar
 * notificações de alertas que sofreram rollback.
 */
@Component
public class AlertaDisparadoListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaDisparadoListener.class);

    private final NotificacaoInternaService notificacoes;
    private final UsuarioRepository usuarios;

    public AlertaDisparadoListener(NotificacaoInternaService notificacoes,
                                   UsuarioRepository usuarios) {
        this.notificacoes = notificacoes;
        this.usuarios = usuarios;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAlertaDisparado(AlertaDisparadoEvent ev) {
        Notificacao.Prioridade prioridade = mapearPrioridade(ev.tipo());
        String titulo = montarTitulo(ev.tipo());
        String mensagem = montarMensagem(ev);
        String linkAcao = "/alertas?disparadoId=" + ev.disparadoId();
        String metadata = "{\"disparadoId\":\"" + ev.disparadoId()
                + "\",\"tipo\":\"" + ev.tipo()
                + "\",\"filialId\":\"" + ev.filialId()
                + "\",\"insumoId\":\"" + ev.insumoId() + "\"}";

        // Página primeiro grupo de usuários ativos. MVP: notifica todos os
        // ADMINs + GERENTES da filial alvo.
        for (int p = 0; p < 10; p++) {
            var lote = usuarios.findAll(p, 100);
            if (lote.isEmpty()) break;
            for (Usuario u : lote) {
                if (!u.ativo()) continue;
                if (!alvoDoAlerta(u, ev.filialId())) continue;
                try {
                    notificacoes.criar(NovaNotificacao.interna(
                            u.id().value(), Notificacao.Tipos.ALERTA_DISPARADO,
                            prioridade, titulo, mensagem, linkAcao, metadata));
                } catch (RuntimeException ex) {
                    // Falha individual não derruba o lote — alertas são
                    // observabilidade, não invariante.
                    log.warn("Falha criando notificação para usuário {}: {}", u.id().value(), ex.getMessage());
                }
            }
            if (lote.size() < 100) break;
        }
    }

    private boolean alvoDoAlerta(Usuario u, java.util.UUID filialAlerta) {
        if (u.perfil() == Perfil.ADMIN) return true;
        if (u.perfil() == Perfil.GERENTE) {
            return u.filialId().map(f -> f.value().equals(filialAlerta)).orElse(false);
        }
        return false;
    }

    private static Notificacao.Prioridade mapearPrioridade(String tipo) {
        return switch (tipo) {
            case "RUPTURA" -> Notificacao.Prioridade.CRITICA;
            case "ESTOQUE_MINIMO_PERCENTUAL", "ESTOQUE_MINIMO_ABSOLUTO" -> Notificacao.Prioridade.AVISO;
            case "VENCIMENTO_PROXIMO_DIAS" -> Notificacao.Prioridade.AVISO;
            default -> Notificacao.Prioridade.INFO;
        };
    }

    private static String montarTitulo(String tipo) {
        return switch (tipo) {
            case "RUPTURA" -> "Ruptura de estoque";
            case "ESTOQUE_MINIMO_PERCENTUAL" -> "Estoque mínimo (percentual) atingido";
            case "ESTOQUE_MINIMO_ABSOLUTO" -> "Estoque mínimo (absoluto) atingido";
            case "VENCIMENTO_PROXIMO_DIAS" -> "Lote próximo do vencimento";
            default -> "Alerta disparado";
        };
    }

    private static String montarMensagem(AlertaDisparadoEvent ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Insumo ").append(shortId(ev.insumoId()))
                .append(" na filial ").append(shortId(ev.filialId()));
        if (ev.saldoNoDisparo() != null) {
            sb.append(" — saldo no disparo: ").append(ev.saldoNoDisparo().toPlainString());
        }
        return sb.toString();
    }

    private static String shortId(java.util.UUID id) {
        return id.toString().substring(0, 8);
    }
}
