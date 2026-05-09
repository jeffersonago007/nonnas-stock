# Configuração de scrape do Prometheus

Master doc 15.2: o backend expõe métricas em `/actuator/prometheus` via Spring Boot Actuator + `micrometer-registry-prometheus`. Este documento descreve como apontar um Prometheus self-hosted (ou Grafana Cloud com Prometheus integrado) para a aplicação.

## Endpoint exposto

`GET http://app.nonnas.com.br/actuator/prometheus` — formato exposto pelo Prometheus Exposition Format. Retorna em texto plano.

A porta interna é 8080 (mesma da API). Em produção, o Nginx faz proxy para `localhost:8080`.

## Acesso restrito

Por default, `/actuator/prometheus` é acessível **sem autenticação** — esperando que a restrição venha do nível de rede (Nginx por IP, security group da VPC).

Em `nginx.conf`, adicione:

```nginx
location /actuator/prometheus {
    # Apenas IP do Prometheus / Grafana Cloud range
    allow 10.0.0.0/8;       # VPC interna
    allow 34.117.7.78/32;   # IP de scrape do Grafana Cloud (exemplo — atualizar com IP real)
    deny all;

    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
}
```

## prometheus.yml (self-hosted)

```yaml
global:
  scrape_interval: 30s
  evaluation_interval: 30s

scrape_configs:
  - job_name: 'nonnas-stock'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
    static_configs:
      - targets: ['app.nonnas.com.br:443']
        labels:
          environment: production
          app: nonnas-stock
    # Se a app usa HTTPS (recomendado em prod), descomentar:
    # scheme: https
```

## Grafana Cloud (free tier)

1. Provisionar uma stack em https://grafana.com/products/cloud/.
2. Em **Connections → Hosted Prometheus → Connect**, copiar a URL de remote_write e o usuário.
3. Configurar o `prometheus.yml` self-hosted para fazer remote_write — não scrape direto, já que Grafana Cloud não acessa endpoints internos.

```yaml
remote_write:
  - url: https://prometheus-prod-01-eu-west-0.grafana.net/api/prom/push
    basic_auth:
      username: <user_id>
      password_file: /etc/prometheus/grafana_cloud_token
```

## Métricas recomendadas a observar

- `http_server_requests_seconds_count` — total de requisições por endpoint.
- `http_server_requests_seconds_sum` / `http_server_requests_seconds_count` → latência média.
- `http_server_requests_seconds_max` — pior caso por janela.
- `jvm_memory_used_bytes` — heap usage.
- `hikaricp_connections_usage_seconds` — uso do pool HikariCP.
- `nonnas_*` — métricas custom (a serem adicionadas via micrometer Counter/Timer em pontos críticos do código — backlog).

## Health checks vs métricas

`/actuator/health` é mais barato (apenas reflete estado do banco e Flyway). `/actuator/prometheus` carrega centenas de séries — usar para observabilidade, não como probe de readiness.

## Próximos passos (T17 ↗ T18)

- Métricas custom de negócio (movimentações/dia, alertas/min) via `MeterRegistry` — aguarda T18.
- Webhook do Grafana Alerting → `/api/v1/notificacoes` (admin) — aguarda T18 quando staging existir.
