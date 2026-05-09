-- ════════════════════════════════════════════════════════════════════════
-- T17 — Sistema de notificações internas
--
-- Master doc 15.4. Toda comunicação operacional acontece dentro do sistema
-- no MVP 1.0. E-mail e WhatsApp ficam para evolução posterior — a coluna
-- canais_destino prepara o caminho.
-- ════════════════════════════════════════════════════════════════════════

CREATE TABLE notificacoes_usuario (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      UUID NOT NULL,
    tipo            VARCHAR(64) NOT NULL,                  -- ALERTA_DISPARADO, TRANSFERENCIA_*, LGPD_*
    prioridade      VARCHAR(16) NOT NULL DEFAULT 'INFO',   -- INFO, AVISO, CRITICA
    titulo          VARCHAR(255) NOT NULL,
    mensagem        TEXT NOT NULL,
    link_acao       VARCHAR(512) NULL,                     -- ex.: /alertas/42
    metadata        TEXT NULL,                             -- JSON serializado
    canais_destino  VARCHAR(255) NOT NULL DEFAULT 'INTERNO', -- CSV: INTERNO, EMAIL, WHATSAPP
    criada_em       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    lida_em         TIMESTAMP WITH TIME ZONE NULL,
    arquivada_em    TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_notificacoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT chk_notificacoes_prioridade CHECK (prioridade IN ('INFO','AVISO','CRITICA')),
    CONSTRAINT chk_notificacoes_titulo_nao_vazio CHECK (LENGTH(TRIM(titulo)) > 0)
);

-- Cobre o caso comum "minhas não-lidas" (badge de polling 30s).
-- Index parcial cobre apenas a-lidas, que é a query crítica do polling.
CREATE INDEX idx_notificacoes_nao_lidas
    ON notificacoes_usuario (usuario_id, criada_em DESC)
    WHERE lida_em IS NULL AND arquivada_em IS NULL;

-- Cobre listagem com filtros (página /notificacoes paginada).
CREATE INDEX idx_notificacoes_usuario_criada
    ON notificacoes_usuario (usuario_id, criada_em DESC);

CREATE INDEX idx_notificacoes_tipo
    ON notificacoes_usuario (tipo, criada_em DESC);
