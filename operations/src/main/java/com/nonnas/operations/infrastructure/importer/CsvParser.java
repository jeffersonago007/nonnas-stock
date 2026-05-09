package com.nonnas.operations.infrastructure.importer;

import com.nonnas.operations.application.carga.PlanilhaCargaInicial;
import com.nonnas.sharedkernel.ValidationException;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parser CSV (OpenCSV). Mesmo schema do XLSX:
 * cabeçalho na primeira linha, ordem fixa
 * insumo_id;unidade_id;numero_lote;quantidade;valor_unitario;data_fabricacao;data_validade
 * (separador ; ou , — detectamos por header).
 */
@Component
class CsvParser implements PlanilhaCargaInicialParser {

    private static final int COL_INSUMO = 0;
    private static final int COL_UNIDADE = 1;
    private static final int COL_LOTE = 2;
    private static final int COL_QTD = 3;
    private static final int COL_VALOR = 4;
    private static final int COL_FAB = 5;
    private static final int COL_VAL = 6;

    @Override
    public PlanilhaCargaInicial parse(InputStream in, String nomeArquivo) {
        byte[] bytes;
        try {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new ValidationException("Falha ao ler arquivo: " + e.getMessage());
        }
        String hash = HashUtil.sha256Hex(bytes);

        char separador = detectarSeparador(bytes);

        List<PlanilhaCargaInicial.Linha> linhas = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
                .withCSVParser(new com.opencsv.CSVParserBuilder().withSeparator(separador).build())
                .build()) {

            String[] linha;
            int idx = 0;
            while ((linha = reader.readNext()) != null) {
                idx++;
                if (idx == 1) continue;  // pula header
                if (linhaTotalVazia(linha)) continue;

                int linhaUsuario = idx;
                if (linha.length < 5) {
                    throw new ValidationException("Linha " + linhaUsuario + ": colunas obrigatórias ausentes");
                }

                UUID insumoId = parseUuid(linha[COL_INSUMO], linhaUsuario, "insumo_id");
                UUID unidadeId = parseUuid(linha[COL_UNIDADE], linhaUsuario, "unidade_id");
                String numeroLote = obrigatorio(linha[COL_LOTE], linhaUsuario, "numero_lote");
                BigDecimal qtd = parseBigDecimal(linha[COL_QTD], linhaUsuario, "quantidade");
                if (qtd.signum() <= 0) {
                    throw new ValidationException("Linha " + linhaUsuario + ": quantidade deve ser positiva");
                }
                BigDecimal valor = parseBigDecimal(linha[COL_VALOR], linhaUsuario, "valor_unitario");
                if (valor.signum() < 0) {
                    throw new ValidationException("Linha " + linhaUsuario + ": valor unitário não pode ser negativo");
                }
                LocalDate fab = linha.length > COL_FAB ? parseDataOpcional(linha[COL_FAB]) : null;
                LocalDate val = linha.length > COL_VAL ? parseDataOpcional(linha[COL_VAL]) : null;

                linhas.add(new PlanilhaCargaInicial.Linha(
                        linhaUsuario, insumoId, unidadeId, numeroLote, qtd, valor, fab, val));
            }
        } catch (IOException | CsvValidationException e) {
            throw new ValidationException("Falha ao processar CSV: " + e.getMessage());
        }

        if (linhas.isEmpty()) {
            throw new ValidationException("Planilha não contém linhas de dados");
        }
        return new PlanilhaCargaInicial(hash, nomeArquivo, linhas);
    }

    private static char detectarSeparador(byte[] bytes) {
        // Olha a primeira linha (até o primeiro \n) para escolher entre ',' e ';'
        int n = Math.min(bytes.length, 4096);
        int virgulas = 0, pvs = 0;
        for (int i = 0; i < n; i++) {
            byte b = bytes[i];
            if (b == '\n') break;
            if (b == ',') virgulas++;
            if (b == ';') pvs++;
        }
        return pvs > virgulas ? ';' : ',';
    }

    private static boolean linhaTotalVazia(String[] linha) {
        for (String c : linha) if (c != null && !c.isBlank()) return false;
        return true;
    }

    private static UUID parseUuid(String s, int linha, String coluna) {
        String t = obrigatorio(s, linha, coluna);
        try { return UUID.fromString(t); }
        catch (IllegalArgumentException e) {
            throw new ValidationException("Linha " + linha + ": " + coluna + " inválido — UUID esperado");
        }
    }

    private static String obrigatorio(String s, int linha, String coluna) {
        if (s == null || s.isBlank()) {
            throw new ValidationException("Linha " + linha + ": " + coluna + " ausente");
        }
        return s.trim();
    }

    private static BigDecimal parseBigDecimal(String s, int linha, String coluna) {
        String t = obrigatorio(s, linha, coluna).replace(",", ".");
        try { return new BigDecimal(t); }
        catch (NumberFormatException e) {
            throw new ValidationException("Linha " + linha + ": " + coluna + " inválido — número esperado");
        }
    }

    private static LocalDate parseDataOpcional(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.trim());  // ISO yyyy-MM-dd
    }
}
