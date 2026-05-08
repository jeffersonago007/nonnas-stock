-- Seed de unidades de medida padrão e conversões globais.
-- Bidirecionais (G→KG e KG→G) NÃO precisam ser inseridas: o ConversorUnidadeService
-- deriva o inverso automaticamente como 1/fator.

INSERT INTO unidades_medida (codigo, nome, tipo, created_at, updated_at) VALUES
    ('G',      'Grama',      'PESO',    NOW(), NOW()),
    ('KG',     'Quilograma', 'PESO',    NOW(), NOW()),
    ('ML',     'Mililitro',  'VOLUME',  NOW(), NOW()),
    ('L',      'Litro',      'VOLUME',  NOW(), NOW()),
    ('UN',     'Unidade',    'UNIDADE', NOW(), NOW()),
    ('CX',     'Caixa',      'UNIDADE', NOW(), NOW()),
    ('PORCAO', 'Porção',     'UNIDADE', NOW(), NOW());

-- Conversões globais (insumo_id NULL).
INSERT INTO conversoes_unidade (unidade_origem_id, unidade_destino_id, fator, created_at)
SELECT
    (SELECT id FROM unidades_medida WHERE codigo = 'KG'),
    (SELECT id FROM unidades_medida WHERE codigo = 'G'),
    1000,
    NOW();

INSERT INTO conversoes_unidade (unidade_origem_id, unidade_destino_id, fator, created_at)
SELECT
    (SELECT id FROM unidades_medida WHERE codigo = 'L'),
    (SELECT id FROM unidades_medida WHERE codigo = 'ML'),
    1000,
    NOW();
