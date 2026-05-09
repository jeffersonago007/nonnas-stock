package com.nonnas.operations.infrastructure.importer;

import com.nonnas.operations.application.carga.PlanilhaCargaInicial;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Roteador entre {@link XlsxParser} e {@link CsvParser} pelo nome do arquivo
 * (extensão). Mantém a interface única exposta ao controller.
 */
@Service
public class PlanilhaImporterService {

    private final XlsxParser xlsx;
    private final CsvParser csv;

    public PlanilhaImporterService(XlsxParser xlsx, CsvParser csv) {
        this.xlsx = xlsx;
        this.csv = csv;
    }

    public PlanilhaCargaInicial parse(InputStream in, String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new ValidationException("Nome do arquivo é obrigatório");
        }
        String lower = nomeArquivo.toLowerCase();
        if (lower.endsWith(".xlsx")) {
            return xlsx.parse(in, nomeArquivo);
        }
        if (lower.endsWith(".csv")) {
            return csv.parse(in, nomeArquivo);
        }
        throw new ValidationException("Formato não suportado: " + nomeArquivo + " (apenas .xlsx ou .csv)");
    }
}
