-- Convenção de cadastro: nomes de produtos vendáveis sempre em UPPERCASE
-- (mesma convenção aplicada em catalog para fornecedores e insumos via V017).
-- Domain (ProdutoVendavel.nome) aplica `.toUpperCase(Locale.ROOT)` no validar;
-- esta migration uniformiza registros pré-existentes.

UPDATE produtos_vendaveis SET nome = UPPER(nome)
WHERE nome <> UPPER(nome);
