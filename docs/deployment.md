# Deployment guide — Nonnas Stock

Guia passo-a-passo para colocar o Nonnas Stock no ar pela primeira vez. Cobre os requisitos do master doc T15: deploy manual no MVP, automação progressiva nos próximos releases.

## 0. Pré-requisitos

- Servidor Linux (Ubuntu 22.04 LTS recomendado) com SSH.
- Docker 24+ e Docker Compose v2.
- Domínio com DNS apontando pro servidor (ex.: `app.nonnas.com.br`).
- Certificado TLS (Let's Encrypt via certbot ou similar).
- PostgreSQL 16 disponível — pode ser RDS/managed ou container Docker.

## 1. Preparar variáveis de ambiente

Crie `/opt/nonnas-stock/.env` com:

```ini
# Banco
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/nonnas
SPRING_DATASOURCE_USERNAME=nonnas_app
SPRING_DATASOURCE_PASSWORD=<gerar senha forte>

# Admin bootstrap (criado no 1º start; muda obrigatoriamente após login)
NONNAS_ADMIN_EMAIL=admin@nonnas.com.br
NONNAS_ADMIN_SENHA=<gerar senha forte>

# JWT
NONNAS_JWT_SECRET=<gerar 256 bits aleatórios>
NONNAS_JWT_EXPIRACAO_MINUTOS=60

# CORS — domínios autorizados
NONNAS_CORS_ALLOWED_ORIGINS=https://app.nonnas.com.br

# Profile
SPRING_PROFILES_ACTIVE=prod
```

Geração de segredos:
```bash
openssl rand -base64 32   # NONNAS_JWT_SECRET
openssl rand -base64 24   # senhas de banco/admin
```

Permissões:
```bash
sudo chown root:root /opt/nonnas-stock/.env
sudo chmod 600 /opt/nonnas-stock/.env
```

## 2. Subir Postgres

Use o `docker-compose.prod.yml` (versionado no repo) ou um banco managed.

Se for via docker-compose:
```bash
cd /opt/nonnas-stock
docker compose -f docker-compose.prod.yml up -d postgres
```

Aguarde ~10s e valide:
```bash
docker compose exec postgres psql -U nonnas_app -d nonnas -c '\l'
```

## 3. Pull da imagem da aplicação

A imagem é publicada em `ghcr.io/jeffersonago007/nonnas-stock:latest` pelo workflow `main.yml` em cada merge.

```bash
docker login ghcr.io   # PAT com `read:packages`
docker pull ghcr.io/jeffersonago007/nonnas-stock:latest
```

## 4. Rodar migrações Flyway

A aplicação roda Flyway no startup automaticamente. **Antes** do primeiro start, garanta que o banco está vazio (Flyway cria `flyway_schema_history`).

Para rodar apenas as migrações sem subir a app inteira (útil para validar):
```bash
docker run --rm --env-file /opt/nonnas-stock/.env \
  ghcr.io/jeffersonago007/nonnas-stock:latest \
  java -jar /app/app.jar --spring.flyway.enabled=true --spring.main.web-application-type=none
```

## 5. Subir aplicação

```bash
cd /opt/nonnas-stock
docker compose -f docker-compose.prod.yml up -d app
```

Saúde:
```bash
curl -fsS http://localhost:8080/actuator/health/liveness
# {"status":"UP"}
```

`AdminBootstrap` cria o usuário admin no primeiro start usando `NONNAS_ADMIN_EMAIL` e `NONNAS_ADMIN_SENHA`. **A senha precisa ser trocada no primeiro login.**

## 6. Servir frontend

Build do frontend:
```bash
cd /opt/nonnas-stock/frontend
npm ci
npm run build
```

Sirva `dist/` via Nginx:

```nginx
server {
  listen 443 ssl http2;
  server_name app.nonnas.com.br;

  ssl_certificate /etc/letsencrypt/live/app.nonnas.com.br/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/app.nonnas.com.br/privkey.pem;

  root /opt/nonnas-stock/frontend/dist;
  index index.html;

  # Front-end SPA: rota não-existente cai no index.html
  location / {
    try_files $uri $uri/ /index.html;
  }

  # API: proxy pro Spring Boot
  location /api/ {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  location /actuator/ {
    proxy_pass http://localhost:8080;
    # Bloquear acesso externo se quiser apenas LB interno fazer probes:
    # allow 10.0.0.0/8; deny all;
  }
}
```

Reload Nginx:
```bash
sudo nginx -t && sudo systemctl reload nginx
```

## 7. Validação final

1. Abra `https://app.nonnas.com.br` no navegador → tela de login da Nonnas Stock.
2. Login com `NONNAS_ADMIN_EMAIL` / senha → forçado a trocar senha.
3. Após troca, dashboard carrega com 4 cards zerados (sem dados ainda).
4. Cadastre uma empresa + filial via UI para validar que CORS, CSRF e CRUDs estão OK.
5. Abra `/actuator/health` (interno) e confirme `db UP`.

## 8. Backups

Configure cron com o script versionado em `scripts/backup-postgres.sh` (ver runbook seção "Backup"). Faça pelo menos 1 restore válido **antes** de considerar produção pronta.

## 9. Próximos passos

- Configurar Sentry / observabilidade (T17).
- Habilitar 2FA TOTP nos admins (T16).
- Setup de DR fora do datacenter principal (T18).
