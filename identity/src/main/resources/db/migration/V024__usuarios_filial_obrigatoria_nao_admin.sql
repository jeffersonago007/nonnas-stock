-- T-RBAC-01: filial é obrigatória para perfis que operam dentro de uma filial.
-- ADMIN segue podendo ter filial_id = NULL (acesso global, sem amarração).
-- Os perfis GERENTE/OPERADOR/CONSULTA precisam estar vinculados a uma filial,
-- caso contrário a escopagem de dados aplicada nos endpoints não tem âncora.
ALTER TABLE usuarios
    ADD CONSTRAINT chk_usuarios_filial_obrigatoria_nao_admin
    CHECK (perfil = 'ADMIN' OR filial_id IS NOT NULL);
