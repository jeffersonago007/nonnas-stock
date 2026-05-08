# ADR 0003 — JWT com refresh token rotation

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** Adendo seção 13.3 + decisão de execução D (sequenciamento)

## Contexto

O sistema precisa autenticar usuários humanos (operadores em filiais, gestores, ADMINs) e, futuramente (onda 1.2), serviços externos via OAuth. As sessões duram um turno inteiro (até 12h) num ambiente de balcão de restaurante onde o operador troca eventualmente sem fazer logout. Tokens de longa duração são alvo atraente: roubo de token = sessão completa.

Alternativas consideradas:
- **Sessão server-side com cookie** (Spring Session + Redis). Mais simples para humanos, mas exige infra adicional (Redis) e atrapalha o futuro suporte a APIs externas.
- **JWT puro de longa duração** (12h+). Simples, sem state, mas roubo de token = sessão inteira comprometida.
- **JWT curto + refresh token rotation**. Token de acesso de 15min, refresh token de 7d que rotaciona a cada uso (uso "queima" o anterior, emite novo). Reuso detectado = revoga toda a família.

## Decisão

Adotamos **JWT de acesso curto (15 minutos) + refresh token rotation com detecção de replay**:

1. Login emite par `(access_token, refresh_token)`. Access TTL 15min, refresh TTL 7 dias.
2. Frontend usa access para chamadas. Quando 401 por expiração, faz `POST /api/v1/auth/refresh` com o refresh token. Recebe par novo. Refresh anterior é invalidado imediatamente (rotação).
3. Cada refresh token é membro de uma **família** (mesma cadeia de origem do login). Se um refresh já invalidado é apresentado novamente, o sistema **revoga toda a família** (todos os tokens descendentes, presentes e futuros) e força o usuário a re-logar. Isso detecta replay sem ambiguidade.
4. **Logout** insere o `jti` do access token na tabela `tokens_revogados` (TTL = expiração do token). Filtro JWT consulta antes de aceitar.

Adicionalmente — e este é o ponto que diverge do calendário original do master doc:

5. **Implementação do refresh rotation acontece em T02** (não em T16). O master doc original colocou refresh rotation em T16 como hardening retroativo. Análise: retrofitar refresh rotation no JwtAuthenticationFilter já consumido pelo frontend (T12–T14) gera retrabalho frontend significativo (~20h). Custo de fazer já em T02: ~10h adicionais. Decisão econômica clara: antecipar. **2FA TOTP, audit log, criptografia de campos sensíveis e endpoints LGPD permanecem em T16** — esses são ortogonais ao login básico e não geram retrabalho.

## Consequências

**Positivas:**
- Roubo de access token expõe ≤ 15 minutos de sessão.
- Roubo de refresh token, se usado pelo atacante, é detectado quando o usuário legítimo tentar a próxima rotação — toda a família é revogada.
- Frontend (T12+) é construído contra a API de auth final, sem retrabalho.

**Negativas:**
- Uma chamada extra a cada 15 minutos por sessão ativa. Aceitável: para 50 usuários simultâneos, são ~3 req/min adicionais — desprezível.
- Lógica de detecção de replay exige tabela `refresh_token_familias` com `parent_jti` e flag `revogada`. Migration em T02.

**Mitigações:**
- Job de limpeza diário remove tokens revogados expirados de `tokens_revogados` e famílias inteiras já expiradas, evitando crescimento descontrolado.
- Métrica Prometheus (T17) conta replays detectados — se subir, é sinal de ataque ou bug.

## Referências

- PROMPT_CLAUDE_CODE.md seção 13.3 (autenticação reforçada).
- ADR 0006 — Sequenciamento pós-adendo (decisão D que move este item para T02).
- IETF RFC 6819 (OAuth 2.0 Threat Model), seção 5.2.2.3 — token theft mitigation.
