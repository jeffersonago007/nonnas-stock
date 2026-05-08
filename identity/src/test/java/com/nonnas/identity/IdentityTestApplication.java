package com.nonnas.identity;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Minimal Spring Boot bootstrap used by identity-module integration tests.
 * Production entry-point arrives in T09 (app module). Lives at the top of
 * the package so {@code @SpringBootTest} auto-discovers it from any
 * sub-package.
 */
@SpringBootApplication(scanBasePackages = "com.nonnas.identity")
@ConfigurationPropertiesScan(basePackages = "com.nonnas.identity")
public class IdentityTestApplication {
}
