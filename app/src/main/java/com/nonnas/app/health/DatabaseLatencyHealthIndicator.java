package com.nonnas.app.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Mede latência de uma query trivial ({@code SELECT 1}) ao banco. Reflete a
 * meta operacional da seção 14.3 do master doc — alvo é < 200ms.
 */
@Component("databaseLatency")
public class DatabaseLatencyHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbc;
    private final Duration thresholdUp;

    public DatabaseLatencyHealthIndicator(JdbcTemplate jdbc,
                                          @Value("${nonnas.health.db-latency.threshold-ms:200}") long thresholdMs) {
        this.jdbc = jdbc;
        this.thresholdUp = Duration.ofMillis(thresholdMs);
    }

    @Override
    public Health health() {
        long t0 = System.nanoTime();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
        } catch (RuntimeException e) {
            return Health.down(e).build();
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);
        Health.Builder b = elapsed.compareTo(thresholdUp) <= 0 ? Health.up() : Health.status("DEGRADED");
        return b.withDetail("latencyMs", elapsed.toMillis())
                .withDetail("thresholdMs", thresholdUp.toMillis())
                .build();
    }
}
