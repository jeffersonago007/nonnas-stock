package com.nonnas.inventory.testsupport;

import com.nonnas.inventory.InventoryTestApplication;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = InventoryTestApplication.class)
public abstract class AbstractInventoryIntegrationTest extends AbstractIntegrationTest {
}
