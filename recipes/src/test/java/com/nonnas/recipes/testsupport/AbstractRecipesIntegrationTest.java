package com.nonnas.recipes.testsupport;

import com.nonnas.recipes.RecipesTestApplication;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = RecipesTestApplication.class)
public abstract class AbstractRecipesIntegrationTest extends AbstractIntegrationTest {
}
