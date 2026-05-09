package com.nonnas.catalog.testsupport;

import com.nonnas.catalog.CatalogTestApplication;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = CatalogTestApplication.class)
public abstract class AbstractCatalogIntegrationTest extends AbstractIntegrationTest {
}
