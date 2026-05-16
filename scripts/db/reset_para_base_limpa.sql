-- Reset do banco para base limpa (mantém só cadastros principais).
-- Executar em desenvolvimento/homologação. NUNCA em produção.
--
-- Preserva:
--   Empresa  : Nonna Paola Bar e Pizzaria LTDA (fab0b60d-86a4-4480-8cc1-94ae0b68d424)
--   Filiais  : Interlagos (2548a238-...) + Vila das Belezas (57846e2e-...)
--   Usuário  : admin@nonnas.com (b4c59f74-...)
--   Categoria: "A classificar" (00000000-...-001 — seed V015)
--   Unidades : seed V004 (G, KG, ML, L, UN, CX, PORCAO) + conversões seed
--   Tabelas intocadas: feature_flags, flyway_schema_history
BEGIN;

-- 1. Transações de estoque
DELETE FROM items_movimentacao;
DELETE FROM movimentacoes;

DELETE FROM itens_transferencia;
DELETE FROM transferencias;

DELETE FROM ajustes_estoque;
DELETE FROM cargas_iniciais;

DELETE FROM notas_fiscais_itens;
DELETE FROM notas_fiscais;
DELETE FROM fornecedor_insumo_depara;

DELETE FROM saldos_lotes;
DELETE FROM lotes;

-- 2. Alertas
DELETE FROM alertas_disparados;
DELETE FROM alertas_config;

-- 3. Receitas e produtos vendáveis
DELETE FROM items_ficha_tecnica;
DELETE FROM fichas_tecnicas;
DELETE FROM produtos_vendaveis;

-- 4. Catálogo (insumos, fornecedores) e conversões por insumo
DELETE FROM insumos_filiais;
DELETE FROM conversoes_unidade WHERE insumo_id IS NOT NULL;
DELETE FROM insumos;

DELETE FROM fornecedores_contatos;
DELETE FROM fornecedores;

-- 5. Dados de auditoria/segurança vinculados a usuários
DELETE FROM tokens_revogados;
DELETE FROM refresh_tokens;
DELETE FROM audit_log;
DELETE FROM notificacoes_usuario;

-- 6. Usuários (CASCADE apaga 2fa, aceites_termos, historico_senhas, notificações restantes)
DELETE FROM usuarios WHERE email <> 'admin@nonnas.com';

-- 7. Filiais — manter apenas Interlagos + Vila das Belezas
DELETE FROM filiais WHERE id NOT IN (
    '2548a238-e7e6-4f0f-816c-77d85c52b165',  -- Interlagos
    '57846e2e-fce9-4736-ad68-b9d9aa47a643'   -- Vila das Belezas
);

-- 8. Empresas — manter apenas Nonna Paola
DELETE FROM empresas WHERE id <> 'fab0b60d-86a4-4480-8cc1-94ae0b68d424';

-- 9. Categorias — manter apenas "A classificar" (seed V015)
DELETE FROM categorias_insumo WHERE id <> '00000000-0000-0000-0000-000000000001';

-- 10. Unidades de medida — manter apenas seed V004
--     Antes, purga conversões que referenciam unidades não-seed (defensivo)
DELETE FROM conversoes_unidade
 WHERE unidade_origem_id  IN (SELECT id FROM unidades_medida WHERE codigo NOT IN ('G','KG','ML','L','UN','CX','PORCAO'))
    OR unidade_destino_id IN (SELECT id FROM unidades_medida WHERE codigo NOT IN ('G','KG','ML','L','UN','CX','PORCAO'));

DELETE FROM unidades_medida WHERE codigo NOT IN ('G','KG','ML','L','UN','CX','PORCAO');

COMMIT;
