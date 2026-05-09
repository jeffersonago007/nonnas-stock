package com.nonnas.app.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Verifica que todas as migrations Flyway aplicaram com sucesso. Útil para
 * readiness probe (Kubernetes não deve mandar tráfego para um pod com
 * migrations pendentes ou em estado de falha).
 */
@Component("migrations")
public class MigrationsHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbc;

    public MigrationsHealthIndicator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Health health() {
        try {
            Integer pending = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE",
                    Integer.class);
            Integer applied = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
                    Integer.class);
            int pendingCount = pending != null ? pending : 0;
            int appliedCount = applied != null ? applied : 0;
            if (pendingCount > 0) {
                return Health.down()
                        .withDetail("failedMigrations", pendingCount)
                        .withDetail("appliedMigrations", appliedCount)
                        .build();
            }
            return Health.up()
                    .withDetail("appliedMigrations", appliedCount)
                    .build();
        } catch (DataAccessException e) {
            return Health.down(e).build();
        }
    }
}
