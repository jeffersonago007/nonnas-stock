package com.nonnas.alerts.testsupport;

import com.nonnas.alerts.AlertsTestApplication;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = AlertsTestApplication.class)
public abstract class AbstractAlertsIntegrationTest extends AbstractIntegrationTest {
}
