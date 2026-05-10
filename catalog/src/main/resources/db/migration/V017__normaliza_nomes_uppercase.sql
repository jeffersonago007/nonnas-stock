-- Convenção de cadastro: nomes de fornecedores e insumos sempre em UPPERCASE
-- (alinha com dados originados de NF-e e elimina duplicidade aparente por
-- diferença de capitalização). Domain (Fornecedor.razaoSocial / Insumo.nome)
-- aplica `.toUpperCase(Locale.ROOT)` no validar; esta migration uniformiza
-- registros pré-existentes.

UPDATE fornecedores SET razao_social = UPPER(razao_social)
WHERE razao_social <> UPPER(razao_social);

UPDATE insumos SET nome = UPPER(nome)
WHERE nome <> UPPER(nome);
