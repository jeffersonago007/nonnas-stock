# LGPD — ROPA (Registro de Operações de Tratamento)

Art. 37 da LGPD obriga o controlador a manter registro das operações. Este documento serve como ROPA tabelado pra auditoria interna e externa (ANPD, certificações).

## 1. Identificação

- **Controlador**: Nonnas Paola — Pizzaria SA — CNPJ <preencher> — endereço <preencher>.
- **DPO**: <preencher>. E-mail: <preencher>. Telefone: <preencher>.
- **Operador**: a própria empresa (sistema é on-prem; nenhum operador externo no MVP).
- **Última revisão**: 2026-05-09.

## 2. Operações registradas

| ID | Atividade               | Categoria de titulares | Categoria de dados                          | Finalidade                                | Base legal                          | Retenção                          |
|----|-------------------------|------------------------|---------------------------------------------|-------------------------------------------|-------------------------------------|-----------------------------------|
| 01 | Cadastro de operador    | Funcionários           | Nome, e-mail, perfil de acesso               | Controle de acesso ao sistema             | Art. 7º V — execução de contrato CLT | 5 anos pós-desligamento (anonimizado) |
| 02 | Login + autenticação    | Funcionários           | E-mail, IP, user-agent, hora                | Segurança da informação                   | Art. 7º IX — interesse legítimo     | 18 meses (audit_log)              |
| 03 | Brute force protection  | Funcionários           | Contagem de falhas, IP                      | Prevenção de invasão                      | Art. 7º IX — interesse legítimo     | TTL conta                         |
| 04 | 2FA TOTP                | Funcionários           | Secret cifrado, backup codes hash           | Segurança da informação                   | Art. 7º IX — interesse legítimo     | TTL conta                         |
| 05 | Trilha de movimentações | Funcionários           | usuario_id em movimentação fiscal           | Cumprimento de obrigação fiscal           | Art. 7º II — obrigação legal        | 5 anos (Art. 195 CTN)             |
| 06 | Aceite de termos        | Funcionários           | Versão dos termos, IP, data                 | Prova de consentimento                    | Art. 7º I — consentimento            | Vida do contrato + 5 anos        |

## 3. Compartilhamento internacional / com terceiros

**Nenhum** no MVP. Sistema on-prem, banco no mesmo datacenter da aplicação. Nenhum dado pessoal sai da rede da empresa.

Se for adicionar operador externo (ex.: Sentry SaaS em T17), atualizar este ROPA com:
- Nome legal do operador.
- País de processamento.
- Garantias contratuais (cláusulas-modelo).
- Data Processing Agreement assinado.

## 4. Medidas de segurança

| Camada            | Medida                                                                 |
|-------------------|------------------------------------------------------------------------|
| Acesso            | JWT 60min + refresh rotation + brute force progressivo + 2FA TOTP      |
| Senha             | BCrypt cost 12 + política de força + histórico anti-reuso              |
| At-rest           | AES-256-GCM em campos sensíveis via `CamposSensiveisConverter`         |
| In-transit        | HTTPS obrigatório (Nginx + Let's Encrypt) + HSTS 1 ano                 |
| Headers           | X-Frame-Options DENY, X-Content-Type-Options nosniff, Referrer no-referrer, CSP |
| Auditoria         | Tabela `audit_log` para eventos não-CRUD (Hibernate Envers em T17 cobre CRUD) |
| Backup            | pg_dump diário criptografado em S3 (off-site opcional)                 |
| Vulnerabilidades  | Trivy no PR + OWASP Dependency Check semanal                           |

## 5. Direitos do titular — canal e prazos

- **Canal**: e-mail do DPO (preencher) ou endpoints `/api/v1/lgpd/*` para o próprio titular logado.
- **Prazo de resposta**: 15 dias (LGPD Art. 19), prorrogável por mais 15 com justificativa.

## 6. Aprovação

| Papel                  | Nome                  | Data        |
|------------------------|-----------------------|-------------|
| Controlador (representante) | Jefferson Agostinho | 2026-05-09  |
| Revisor jurídico       | _<preencher>_         | _pendente_  |
| DPO                    | _<preencher>_         | _pendente_  |
