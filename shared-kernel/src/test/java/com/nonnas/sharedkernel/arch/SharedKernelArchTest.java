package com.nonnas.sharedkernel.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architectural invariants for shared-kernel.
 *
 * <p>shared-kernel is the most foundational module and must remain free of
 * any framework or infrastructure dependency so that domain layers in
 * higher modules can include it without dragging Spring/JPA/Lombok into
 * their tests.
 *
 * <p>These rules are intentionally local: the project-wide ArchUnit suite
 * (T10) will declare the same invariants over the entire reactor.
 */
@AnalyzeClasses(packages = "com.nonnas.sharedkernel", importOptions = ImportOption.DoNotIncludeTests.class)
class SharedKernelArchTest {

    @ArchTest
    static final ArchRule noSpringDependency = noClasses()
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule noJpaDependency = noClasses()
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule noLombokDependency = noClasses()
            .should().dependOnClassesThat().resideInAPackage("lombok..");

    @ArchTest
    static final ArchRule noLegacyDateDependency = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Date")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("java.util.Calendar");
}
