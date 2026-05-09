package com.nonnas.operations;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.nonnas.operations", "com.nonnas.inventory"})
@EntityScan(basePackages = {"com.nonnas.operations", "com.nonnas.inventory"})
@EnableJpaRepositories(basePackages = {"com.nonnas.operations", "com.nonnas.inventory"})
@ConfigurationPropertiesScan(basePackages = {"com.nonnas.operations", "com.nonnas.inventory"})
public class OperationsTestApplication {
}
