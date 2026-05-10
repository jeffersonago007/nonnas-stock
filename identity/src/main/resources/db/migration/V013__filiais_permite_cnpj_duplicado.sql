-- Regra de negócio: filiais de uma mesma rede compartilham o CNPJ da matriz
-- (caso Nonnas Paola — múltiplas lojas operando sob o mesmo CNPJ fiscal).
-- O constraint UNIQUE original em V001 estava bloqueando o cadastro real.
-- Drop apenas do UNIQUE; o CHECK de formato (14 dígitos) permanece.

ALTER TABLE filiais DROP CONSTRAINT IF EXISTS uq_filiais_cnpj;
