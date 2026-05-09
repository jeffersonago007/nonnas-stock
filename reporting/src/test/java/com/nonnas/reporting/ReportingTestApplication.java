package com.nonnas.reporting;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.nonnas.reporting")
@ConfigurationPropertiesScan(basePackages = "com.nonnas.reporting")
public class ReportingTestApplication {
}
