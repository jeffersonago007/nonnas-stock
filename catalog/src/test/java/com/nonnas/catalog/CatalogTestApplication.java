package com.nonnas.catalog;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.nonnas.catalog")
@ConfigurationPropertiesScan(basePackages = "com.nonnas.catalog")
public class CatalogTestApplication {
}
