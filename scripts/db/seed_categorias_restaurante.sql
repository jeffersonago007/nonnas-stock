-- Seed de categorias padrão para restaurante (bar + pizzaria).
-- Baseado em ERPs/POS de mercado (Linx Food, Consinco, Sankhya, Goomer).
-- A categoria "A classificar" (seed V015) já existe e NÃO é tocada.
BEGIN;

INSERT INTO categorias_insumo (nome, ativa, created_at, updated_at) VALUES
    ('Carnes',                          true, now(), now()),
    ('Aves',                            true, now(), now()),
    ('Peixes e Frutos do Mar',          true, now(), now()),
    ('Frios e Embutidos',               true, now(), now()),
    ('Laticínios',                      true, now(), now()),
    ('Ovos',                            true, now(), now()),
    ('Hortifrúti',                      true, now(), now()),
    ('Massas e Farinhas',               true, now(), now()),
    ('Pães e Panificação',              true, now(), now()),
    ('Grãos, Cereais e Leguminosas',    true, now(), now()),
    ('Óleos e Gorduras',                true, now(), now()),
    ('Temperos, Ervas e Condimentos',   true, now(), now()),
    ('Molhos e Conservas',              true, now(), now()),
    ('Enlatados',                       true, now(), now()),
    ('Congelados',                      true, now(), now()),
    ('Doces e Confeitaria',             true, now(), now()),
    ('Bebidas Não Alcoólicas',          true, now(), now()),
    ('Bebidas Alcoólicas',              true, now(), now()),
    ('Embalagens',                      true, now(), now()),
    ('Descartáveis',                    true, now(), now()),
    ('Produtos de Limpeza',             true, now(), now());

COMMIT;
