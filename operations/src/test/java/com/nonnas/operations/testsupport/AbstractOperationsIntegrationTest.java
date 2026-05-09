package com.nonnas.operations.testsupport;

import com.nonnas.operations.OperationsTestApplication;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = OperationsTestApplication.class)
public abstract class AbstractOperationsIntegrationTest extends AbstractIntegrationTest {
}
