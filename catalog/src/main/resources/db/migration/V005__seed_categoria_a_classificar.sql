-- Seed da categoria fixa "A classificar" usada pelo importador de NF-e (T20).
-- UUID determinístico para que o use case ProcessarNotaFiscal possa referenciá-la
-- via constante sem lookup por nome (evita coupling com string mutável).

INSERT INTO categorias_insumo (id, categoria_pai_id, nome, ativa, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    NULL,
    'A classificar',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;
