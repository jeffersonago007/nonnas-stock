package com.nonnas.operations.infrastructure.importer;

import com.nonnas.operations.application.carga.PlanilhaCargaInicial;
import com.nonnas.sharedkernel.ValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parser XLSX (Apache POI). Schema fixo de colunas, linha 0 = cabeçalho:
 * <pre>
 *   A: insumo_id (UUID)
 *   B: unidade_id (UUID)
 *   C: numero_lote (texto)
 *   D: quantidade (decimal &gt; 0)
 *   E: valor_unitario (decimal &ge; 0)
 *   F: data_fabricacao (data, opcional)
 *   G: data_validade (data, opcional)
 * </pre>
 */
@Component
class XlsxParser implements PlanilhaCargaInicialParser {

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

        List<PlanilhaCargaInicial.Linha> linhas = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter df = new DataFormatter();
            int last = sheet.getLastRowNum();
            for (int rowIdx = 1; rowIdx <= last; rowIdx++) {
                Row r = sheet.getRow(rowIdx);
                if (r == null || isLinhaVazia(r)) continue;
                int linhaUsuario = rowIdx + 1;  // 1-based para mensagens

                UUID insumoId = parseUuid(r.getCell(COL_INSUMO), df, linhaUsuario, "insumo_id");
                UUID unidadeId = parseUuid(r.getCell(COL_UNIDADE), df, linhaUsuario, "unidade_id");
                String numeroLote = parseStringObrigatorio(r.getCell(COL_LOTE), df, linhaUsuario, "numero_lote");
                BigDecimal qtd = parseBigDecimalObrigatorio(r.getCell(COL_QTD), df, linhaUsuario, "quantidade");
                if (qtd.signum() <= 0) {
                    throw new ValidationException("Linha " + linhaUsuario + ": quantidade deve ser positiva");
                }
                BigDecimal valor = parseBigDecimalObrigatorio(r.getCell(COL_VALOR), df, linhaUsuario, "valor_unitario");
                if (valor.signum() < 0) {
                    throw new ValidationException("Linha " + linhaUsuario + ": valor unitário não pode ser negativo");
                }
                LocalDate fab = parseLocalDateOpcional(r.getCell(COL_FAB), df);
                LocalDate val = parseLocalDateOpcional(r.getCell(COL_VAL), df);

                linhas.add(new PlanilhaCargaInicial.Linha(
                        linhaUsuario, insumoId, unidadeId, numeroLote, qtd, valor, fab, val));
            }
        } catch (IOException e) {
            throw new ValidationException("Falha ao processar XLSX: " + e.getMessage());
        }

        if (linhas.isEmpty()) {
            throw new ValidationException("Planilha não contém linhas de dados");
        }
        return new PlanilhaCargaInicial(hash, nomeArquivo, linhas);
    }

    private static boolean isLinhaVazia(Row r) {
        for (int c = 0; c <= COL_VAL; c++) {
            Cell cell = r.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private static UUID parseUuid(Cell c, DataFormatter df, int linha, String coluna) {
        String txt = parseStringObrigatorio(c, df, linha, coluna);
        try {
            return UUID.fromString(txt);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Linha " + linha + ": " + coluna + " inválido — UUID esperado");
        }
    }

    private static String parseStringObrigatorio(Cell c, DataFormatter df, int linha, String coluna) {
        if (c == null) {
            throw new ValidationException("Linha " + linha + ": " + coluna + " ausente");
        }
        String s = df.formatCellValue(c).trim();
        if (s.isBlank()) {
            throw new ValidationException("Linha " + linha + ": " + coluna + " ausente");
        }
        return s;
    }

    private static BigDecimal parseBigDecimalObrigatorio(Cell c, DataFormatter df, int linha, String coluna) {
        if (c == null || c.getCellType() == CellType.BLANK) {
            throw new ValidationException("Linha " + linha + ": " + coluna + " ausente");
        }
        if (c.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(c.getNumericCellValue());
        }
        String txt = df.formatCellValue(c).replace(",", ".").trim();
        try {
            return new BigDecimal(txt);
        } catch (NumberFormatException e) {
            throw new ValidationException("Linha " + linha + ": " + coluna + " inválido — número esperado");
        }
    }

    private static LocalDate parseLocalDateOpcional(Cell c, DataFormatter df) {
        if (c == null || c.getCellType() == CellType.BLANK) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getLocalDateTimeCellValue().toLocalDate();
        }
        String txt = df.formatCellValue(c).trim();
        if (txt.isBlank()) return null;
        return LocalDate.parse(txt);  // ISO yyyy-MM-dd
    }
}
