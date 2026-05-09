package com.nonnas.alerts;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.nonnas.alerts", "com.nonnas.inventory", "com.nonnas.catalog", "com.nonnas.web"})
@EntityScan(basePackages = {"com.nonnas.alerts", "com.nonnas.inventory", "com.nonnas.catalog"})
@EnableJpaRepositories(basePackages = {"com.nonnas.alerts", "com.nonnas.inventory", "com.nonnas.catalog"})
@ConfigurationPropertiesScan(basePackages = {"com.nonnas.alerts", "com.nonnas.inventory", "com.nonnas.catalog"})
public class AlertsTestApplication {
}
