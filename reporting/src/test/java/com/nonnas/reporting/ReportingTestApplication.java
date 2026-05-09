package com.nonnas.reporting;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.nonnas.reporting", "com.nonnas.web"})
@ConfigurationPropertiesScan(basePackages = "com.nonnas.reporting")
public class ReportingTestApplication {
}
