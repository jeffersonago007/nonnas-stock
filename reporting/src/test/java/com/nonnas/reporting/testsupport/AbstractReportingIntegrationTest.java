package com.nonnas.reporting.testsupport;

import com.nonnas.reporting.ReportingTestApplication;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = ReportingTestApplication.class)
public abstract class AbstractReportingIntegrationTest extends AbstractIntegrationTest {
}
