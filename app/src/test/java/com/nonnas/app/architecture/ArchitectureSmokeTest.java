package com.nonnas.app.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garantias arquiteturais mínimas do T09:
 * <ul>
 *   <li>Bounded contexts não atravessam fronteiras exceto pelas dependências
 *       declaradas em Maven (recipes/operations/alerts → inventory-core,
 *       alerts → catalog).</li>
 *   <li>{@code com.nonnas.app} é o único pacote autorizado a juntar todos
 *       os módulos — verificado pelo fato de que cada módulo individual
 *       não importa de outros módulos não-declarados.</li>
 * </ul>
 *
 * <p>T10 expande isto em {@code quality-tests/} com camadas (domain → app →
 * infra), proibições de Lombok no shared-kernel etc.
 */
@AnalyzeClasses(packages = "com.nonnas")
class ArchitectureSmokeTest {

    @ArchTest
    static final ArchRule identity_naoCruzaFronteiraNenhuma = noClasses()
            .that().resideInAPackage("com.nonnas.identity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nonnas.catalog..",
                    "com.nonnas.inventory..",
                    "com.nonnas.recipes..",
                    "com.nonnas.operations..",
                    "com.nonnas.alerts..",
                    "com.nonnas.reporting..");

    @ArchTest
    static final ArchRule catalog_naoCruzaFronteiraNenhuma = noClasses()
            .that().resideInAPackage("com.nonnas.catalog..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nonnas.identity..",
                    "com.nonnas.inventory..",
                    "com.nonnas.recipes..",
                    "com.nonnas.operations..",
                    "com.nonnas.alerts..",
                    "com.nonnas.reporting..");

    @ArchTest
    static final ArchRule inventoryCore_naoCruzaFronteiraNenhuma = noClasses()
            .that().resideInAPackage("com.nonnas.inventory..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nonnas.identity..",
                    "com.nonnas.catalog..",
                    "com.nonnas.recipes..",
                    "com.nonnas.operations..",
                    "com.nonnas.alerts..",
                    "com.nonnas.reporting..");

    @ArchTest
    static final ArchRule recipes_soDependeDeInventoryCore = noClasses()
            .that().resideInAPackage("com.nonnas.recipes..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nonnas.identity..",
                    "com.nonnas.catalog..",
                    "com.nonnas.operations..",
                    "com.nonnas.alerts..",
                    "com.nonnas.reporting..");

    @ArchTest
    static final ArchRule operations_soDependeDeInventoryCore = noClasses()
            .that().resideInAPackage("com.nonnas.operations..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nonnas.identity..",
                    "com.nonnas.catalog..",
                    "com.nonnas.recipes..",
                    "com.nonnas.alerts..",
                    "com.nonnas.reporting..");

    @ArchTest
    static final ArchRule alerts_soDependeDeInventoryCoreECatalog = noClasses()
            .that().resideInAPackage("com.nonnas.alerts..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nonnas.identity..",
                    "com.nonnas.recipes..",
                    "com.nonnas.operations..",
                    "com.nonnas.reporting..");

    /**
     * Reporting usa SQL nativo cross-schema (ADR 0010). Em Java, ele NÃO
     * importa classes dos outros bounded contexts — só shared-kernel e
     * web-commons.
     */
    @ArchTest
    static final ArchRule reporting_naoImportaJavaDeOutrosModulos = noClasses()
            .that().resideInAPackage("com.nonnas.reporting..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nonnas.identity..",
                    "com.nonnas.catalog..",
                    "com.nonnas.inventory..",
                    "com.nonnas.recipes..",
                    "com.nonnas.operations..",
                    "com.nonnas.alerts..");
}
