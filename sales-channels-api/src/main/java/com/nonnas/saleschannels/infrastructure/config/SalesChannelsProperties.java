package com.nonnas.saleschannels.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração transversal dos canais de venda.
 *
 * <p>O polling (consumir eventos do canal) e o processar (baixar estoque
 * + confirmar no canal) podem ser ligados independentemente via
 * {@code enabled}. Mesmo desligados, os endpoints manuais
 * {@code POST /api/v1/canais/{tipo}/poll-now} (ADMIN) e
 * {@code POST /api/v1/canais/processar-pendentes} (ADMIN/GERENTE)
 * continuam funcionando — úteis em dev/CI e para retry pela UI.
 *
 * <p>{@code timeout} aplica ao RestClient usado pelos adapters HTTP.
 */
@ConfigurationProperties(prefix = "nonnas.canais")
public record SalesChannelsProperties(Polling polling, Processar processar, Http http) {

    public record Polling(boolean enabled, Duration interval, int batchSize) {}

    public record Processar(boolean enabled, Duration interval, int batchSize) {}

    public record Http(Duration connectTimeout, Duration readTimeout) {}

    public SalesChannelsProperties {
        if (polling == null) {
            polling = new Polling(false, Duration.ofSeconds(30), 50);
        } else {
            polling = new Polling(
                    polling.enabled(),
                    polling.interval() != null ? polling.interval() : Duration.ofSeconds(30),
                    polling.batchSize() > 0 ? polling.batchSize() : 50);
        }
        if (processar == null) {
            processar = new Processar(false, Duration.ofSeconds(60), 50);
        } else {
            processar = new Processar(
                    processar.enabled(),
                    processar.interval() != null ? processar.interval() : Duration.ofSeconds(60),
                    processar.batchSize() > 0 ? processar.batchSize() : 50);
        }
        if (http == null) {
            http = new Http(Duration.ofSeconds(5), Duration.ofSeconds(15));
        } else {
            http = new Http(
                    http.connectTimeout() != null ? http.connectTimeout() : Duration.ofSeconds(5),
                    http.readTimeout() != null ? http.readTimeout() : Duration.ofSeconds(15));
        }
    }
}
