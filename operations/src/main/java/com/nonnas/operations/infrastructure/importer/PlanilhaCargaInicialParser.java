package com.nonnas.operations.infrastructure.importer;

import com.nonnas.operations.application.carga.PlanilhaCargaInicial;
import com.nonnas.sharedkernel.ValidationException;

import java.io.InputStream;

/**
 * Contrato do parser de planilha. Implementações: {@link XlsxParser} (POI)
 * e {@link CsvParser} (OpenCSV). Selecionado pelo {@link PlanilhaImporterService}
 * com base na extensão do nome do arquivo.
 */
public interface PlanilhaCargaInicialParser {

    /** Lê o stream e devolve um modelo normalizado. Lança {@link ValidationException} para erros de formato. */
    PlanilhaCargaInicial parse(InputStream in, String nomeArquivo);
}
