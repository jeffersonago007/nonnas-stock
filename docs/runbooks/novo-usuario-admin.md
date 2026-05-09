# Runbook — Provisionar novo usuário ADMIN

**Quando usar**: novo membro da equipe (Jeff, Ewerton, Luiza...) precisa de acesso administrativo.

**Pré-requisitos**:
- Você é ADMIN existente.
- O futuro usuário tem um e-mail corporativo ativo.
- Senha temporária forte gerada (ver passo 2).

## Procedimento

### 1. Validar a necessidade

ADMIN tem poder absoluto: cria/desativa qualquer usuário, vê dados de qualquer filial, executa exclusão LGPD. Considerar se **GERENTE** não atende. ADMIN só pra:
- Pessoa que precisa atravessar todas as filiais (controller, auditoria interna).
- Operação técnica (deploy, troubleshooting, restore).

### 2. Gerar senha temporária forte

```bash
# 24 chars, mistura letras/números/símbolos.
openssl rand -base64 18
```

Anotar com calma — você só vai mostrar uma vez. Compartilhar via canal seguro (1Password share link, SMS pra celular pessoal, NUNCA email/Slack).

### 3. Criar usuário via UI

1. Login como ADMIN existente.
2. Cadastros → Usuários (TODO: implementar essa página — ver gap T13.5).
3. **Provisório, sem UI**: usar API direta.

```bash
ADMIN_TOKEN=$(curl -fsS -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@nonnas.com","senha":"<sua-senha>"}' \
    | jq -r .accessToken)

curl -fsS -X POST http://localhost:8080/api/v1/usuarios \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "nome": "Nome Completo",
      "email": "novo.admin@nonnas.com",
      "senha": "<senha-temporaria-gerada>",
      "perfil": "ADMIN"
    }'
```

ADMIN não precisa de filialId associada (pode atravessar todas).

### 4. Validar criação

```bash
curl -fsS http://localhost:8080/api/v1/usuarios \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    | jq '.[] | select(.email == "novo.admin@nonnas.com")'
```

Deve aparecer com `perfil: "ADMIN"` e `ativo: true`.

### 5. Comunicar à pessoa

Mensagem padrão (ajustar):

> Oi <Nome>!
>
> Seu acesso ao Nonnas Stock como ADMIN está pronto:
> - URL: https://app.nonnas.com.br
> - E-mail: novo.admin@nonnas.com
> - Senha temporária: <senha>
>
> No primeiro login você vai ser obrigado a trocar essa senha. Política exige:
> - 10+ caracteres
> - Pelo menos 1 letra, 1 número, 1 caractere especial
> - Não pode reusar nenhuma das últimas 5
>
> Por favor, configure 2FA em "Configurações → Segurança" assim que entrar.
> Não compartilhe senha por email/Slack — usa 1Password ou Bitwarden.
>
> Qualquer dúvida, me chama.

### 6. Auditoria

A criação automaticamente vira entrada no `audit_log`:

```sql
SELECT * FROM identity.audit_log
 WHERE event_type = 'USUARIO_CRIADO'
   AND occurred_at > NOW() - INTERVAL '5 minutes'
 ORDER BY occurred_at DESC;
```

Conferir que ficou registrado. Se não ficou, abrir bug.

## Casos especiais

### Esqueci a senha temporária antes de mandar

- Logar como outro ADMIN.
- Aguardar — o sistema ainda não tem "admin reset password endpoint" (gap T18+).
- **Workaround**: deletar e recriar o usuário com nova senha temporária.

### O usuário não recebeu / não acessou em 7 dias

Política de segurança: se senha temporária não foi usada em 7 dias, considerar que vazou e:

```bash
# Desativar
curl -fsS -X PATCH http://localhost:8080/api/v1/usuarios/<id>/desativar \
    -H "Authorization: Bearer $ADMIN_TOKEN"
```

Recriar com nova senha temporária.

### Despromover ADMIN para GERENTE

Hoje não há endpoint de mudança de perfil — gap. Workaround SQL:

```sql
UPDATE identity.usuarios
   SET perfil = 'GERENTE',
       updated_at = NOW()
 WHERE id = '<uuid>';

-- Audita manualmente
INSERT INTO identity.audit_log (event_type, actor_id, target_kind, target_id, metadata)
VALUES ('PERFIL_ALTERADO', '<seu-id>', 'USUARIO', '<id-target>',
        '{"de":"ADMIN","para":"GERENTE","motivo":"<descrever>"}');
```

Forçar logout do usuário pra invalidar tokens antigos:

```sql
UPDATE identity.refresh_tokens SET revoked_at = NOW() WHERE usuario_id = '<id>';
```

## Pós-criação

- O novo usuário aparece em `/api/v1/usuarios` listado.
- 1º login força troca de senha (T16).
- Pelo menos uma vez por trimestre, revisar lista de ADMINs — desativar quem saiu da empresa, despromover quem mudou de função.
