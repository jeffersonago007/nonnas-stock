package com.nonnas.quality.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras de isolamento entre bounded contexts. Cada módulo só pode
 * depender daqueles que declarou em Maven.
 *
 * <p>Mapa de dependências (refs ADRs 0008, 0009, 0010):
 * <ul>
 *   <li>identity → shared-kernel, web-commons</li>
 *   <li>catalog → shared-kernel, web-commons</li>
 *   <li>inventory-core → shared-kernel, web-commons</li>
 *   <li>recipes → shared-kernel, web-commons, inventory-core (ADR 0008)</li>
 *   <li>operations → shared-kernel, web-commons, inventory-core (ADR 0009)</li>
 *   <li>alerts → shared-kernel, web-commons, inventory-core, catalog</li>
 *   <li>reporting → shared-kernel, web-commons (ADR 0010 — outras
 *       dependências Maven existem só para encadear migrations Flyway, mas
 *       o código Java não importa)</li>
 *   <li>nfe-importer → shared-kernel, web-commons, catalog, inventory-core,
 *       operations (T20 — orquestrador de importação resolve fornecedor/
 *       insumo via catalog e delega persistência para operations)</li>
 *   <li>app → todos (único agregador autorizado)</li>
 * </ul>
 *
 * <p>Mantém também a regra "shared-kernel main é zero-deps de Spring/JPA/Lombok".
 */
@AnalyzeClasses(
        packages = "com.nonnas",
        importOptions = ImportOption.DoNotIncludeTests.class)
class BoundedContextIsolationTest {

    @ArchTest
    static final ArchRule sharedKernel_eZeroDepsDeFrameworks = noClasses()
            .that().resideInAPackage("com.nonnas.sharedkernel..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "lombok..",
                    "org.hibernate..");

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
     * Reporting usa SQL nativo cross-schema (ADR 0010); em Java, NÃO importa
     * classes dos outros bounded contexts — só shared-kernel e web-commons.
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

    /**
     * nfe-importer (T20) é orquestrador autorizado a cruzar fronteiras de
     * catalog (criar/buscar Fornecedor e Insumo), inventory-core (entidades
     * referenciadas indiretamente) e operations (LancarNotaFiscalUseCase).
     * NÃO pode acessar identity/recipes/alerts/reporting.
     */
    @ArchTest
    static final ArchRule nfeImporter_naoAcessaContextosForaDoEscopo = noClasses()
            .that().resideInAPackage("com.nonnas.nfeimporter..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nonnas.identity..",
                    "com.nonnas.recipes..",
                    "com.nonnas.alerts..",
                    "com.nonnas.reporting..");

    /**
     * T-LOT-09 (adendo lote opcional): a criação do lote AGREGADOR só pode
     * passar pelo {@code BuscarOuCriarLoteAgregadorUseCase}. Nenhum outro
     * lugar pode chamar {@code Lote.novoAgregador} diretamente — protege a
     * idempotência (unique partial index) e mantém o regime do insumo
     * estritamente orquestrado. Como {@code novoAgregador} só existe em
     * {@code Lote}, basta filtrar pelo nome do método.
     */
    @ArchTest
    static final ArchRule loteAgregador_soAcessadoViaUseCase = noClasses()
            .that().resideOutsideOfPackages(
                    "com.nonnas.inventory.application.movimentacao..",
                    "com.nonnas.inventory.domain..")
            .should().callMethodWhere(
                    com.tngtech.archunit.core.domain.JavaCall.Predicates.target(
                            com.tngtech.archunit.core.domain.properties.HasName.Predicates.name("novoAgregador")));
}
