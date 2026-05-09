package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.application.notifications.Notificacao;
import com.nonnas.identity.application.ports.NotificacaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class NotificacaoRepositoryImpl implements NotificacaoRepository {

    private final SpringDataNotificacaoRepository jpa;

    NotificacaoRepositoryImpl(SpringDataNotificacaoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Notificacao save(Notificacao n) {
        NotificacaoEntity e = toEntity(n);
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Notificacao> findById(UUID id) {
        return jpa.findById(id).map(NotificacaoRepositoryImpl::toDomain);
    }

    @Override
    public List<Notificacao> findFiltered(UUID usuarioId, String tipo, boolean incluirArquivadas,
                                          boolean somenteNaoLidas, int page, int size) {
        return jpa.findFiltered(usuarioId, tipo, incluirArquivadas, somenteNaoLidas,
                        PageRequest.of(page, size))
                .map(NotificacaoRepositoryImpl::toDomain)
                .getContent();
    }

    @Override
    public long countNaoLidas(UUID usuarioId) {
        return jpa.countNaoLidas(usuarioId);
    }

    @Override
    public void marcarLida(UUID id, Instant agora) {
        jpa.marcarLida(id, agora);
    }

    @Override
    public int marcarTodasLidas(UUID usuarioId, Instant agora) {
        return jpa.marcarTodasLidas(usuarioId, agora);
    }

    @Override
    public void arquivar(UUID id, Instant agora) {
        jpa.arquivar(id, agora);
    }

    private static NotificacaoEntity toEntity(Notificacao n) {
        NotificacaoEntity e = new NotificacaoEntity();
        e.setId(n.id() != null ? n.id() : UUID.randomUUID());
        e.setUsuarioId(n.usuarioId());
        e.setTipo(n.tipo());
        e.setPrioridade(n.prioridade().name());
        e.setTitulo(n.titulo());
        e.setMensagem(n.mensagem());
        e.setLinkAcao(n.linkAcao());
        e.setMetadata(n.metadataJson());
        e.setCanaisDestino(n.canaisDestino());
        e.setCriadaEm(n.criadaEm());
        e.setLidaEm(n.lidaEm());
        e.setArquivadaEm(n.arquivadaEm());
        return e;
    }

    private static Notificacao toDomain(NotificacaoEntity e) {
        return new Notificacao(
                e.getId(), e.getUsuarioId(), e.getTipo(),
                Notificacao.Prioridade.valueOf(e.getPrioridade()),
                e.getTitulo(), e.getMensagem(), e.getLinkAcao(),
                e.getMetadata(), e.getCanaisDestino(),
                e.getCriadaEm(), e.getLidaEm(), e.getArquivadaEm());
    }
}
