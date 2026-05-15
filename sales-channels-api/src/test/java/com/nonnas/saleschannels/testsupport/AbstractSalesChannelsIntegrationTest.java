package com.nonnas.saleschannels.testsupport;

import com.nonnas.saleschannels.SalesChannelsTestApplication;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = SalesChannelsTestApplication.class)
public abstract class AbstractSalesChannelsIntegrationTest extends AbstractIntegrationTest {
}
