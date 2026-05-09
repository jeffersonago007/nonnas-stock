package com.nonnas.sharedkernel.testsupport;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base para ITs com Postgres embarcado (Zonky) e profile {@code test}.
 * Subclasses adicionam o {@code @SpringBootTest(classes = ...)} específico
 * do bounded context — Spring Test compõe as meta-anotações na hierarquia.
 *
 * <p>ADR 0007: Zonky em vez de Testcontainers (sem Docker no ambiente).
 * ADR 0011: este AbstractIntegrationTest centraliza setup, distribuído via
 * shared-kernel test-jar.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
public abstract class AbstractIntegrationTest {
}
