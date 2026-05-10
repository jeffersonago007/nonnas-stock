package com.nonnas.nfeimporter.testsupport;

import com.nonnas.nfeimporter.NfeImporterTestApplication;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = NfeImporterTestApplication.class)
public abstract class AbstractNfeImporterIntegrationTest extends AbstractIntegrationTest {
}
