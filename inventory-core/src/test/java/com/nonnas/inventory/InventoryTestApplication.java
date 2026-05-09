package com.nonnas.inventory;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.nonnas.inventory", "com.nonnas.web"})
@ConfigurationPropertiesScan(basePackages = "com.nonnas.inventory")
public class InventoryTestApplication {
}
