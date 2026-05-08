package com.nonnas.inventory;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.nonnas.inventory")
@ConfigurationPropertiesScan(basePackages = "com.nonnas.inventory")
public class InventoryTestApplication {
}
