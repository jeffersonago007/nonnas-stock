package com.nonnas.operations.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "nonnas.operations")
public record OperationsProperties(Ajuste ajuste) {

    public record Ajuste(BigDecimal thresholdAprovacao) {}

    public OperationsProperties {
        if (ajuste == null) {
            ajuste = new Ajuste(new BigDecimal("50"));
        } else if (ajuste.thresholdAprovacao() == null) {
            ajuste = new Ajuste(new BigDecimal("50"));
        }
    }
}
