package com.nonnas.e2e.fixtures;

/**
 * CNPJs válidos (com dígitos verificadores corretos) reservados para fixtures
 * de E2E. Diferentes dos usados em ITs do backend pra evitar colisão se
 * compartilharem o mesmo banco de dev.
 */
public final class Cnpjs {

    public static final String EMPRESA_E2E = "22333444000181";
    public static final String FILIAL_E2E_PRINCIPAL = "33444555000181";
    public static final String FILIAL_E2E_SECUNDARIA = "84411474000116";
    public static final String FORNECEDOR_E2E = "76024099000123";

    private Cnpjs() {}
}
