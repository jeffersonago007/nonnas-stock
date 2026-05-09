package com.nonnas.reporting.infrastructure.persistence;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import com.nonnas.reporting.domain.ClasseABC;
import com.nonnas.reporting.domain.CurvaABCItem;
import com.nonnas.reporting.domain.DivergenciaInventarioItem;
import com.nonnas.reporting.domain.MovimentacaoPorPeriodoItem;
import com.nonnas.reporting.domain.PeriodoFiltro;
import com.nonnas.reporting.domain.PosicaoEstoqueItem;
import com.nonnas.reporting.domain.RupturaIminenteItem;
import com.nonnas.reporting.domain.SituacaoRuptura;
import com.nonnas.reporting.domain.VencimentoProximoItem;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class RelatorioQueriesJdbc implements RelatorioQueries {

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public RelatorioQueriesJdbc(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public List<PosicaoEstoqueItem> posicaoEstoque(UUID filialId, UUID categoriaId, int page, int size) {
        String sql = """
                SELECT sl.filial_id           AS filial_id,
                       i.id                   AS insumo_id,
                       i.codigo               AS insumo_codigo,
                       i.nome                 AS insumo_nome,
                       SUM(sl.quantidade_base)                       AS saldo_total,
                       SUM(sl.quantidade_base * l.valor_unitario)    AS valor_estoque,
                       COUNT(DISTINCT l.id)                          AS quantidade_lotes
                FROM saldos_lotes sl
                JOIN lotes   l ON l.id  = sl.lote_id
                JOIN insumos i ON i.id  = l.insumo_id
                WHERE (CAST(:filialId AS uuid)    IS NULL OR sl.filial_id   = CAST(:filialId AS uuid))
                  AND (CAST(:categoriaId AS uuid) IS NULL OR i.categoria_id = CAST(:categoriaId AS uuid))
                  AND sl.quantidade_base > 0
                GROUP BY sl.filial_id, i.id, i.codigo, i.nome
                ORDER BY i.nome
                LIMIT :size OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("filialId", filialId)
                .addValue("categoriaId", categoriaId)
                .addValue("size", size)
                .addValue("offset", offset(page, size));
        return jdbc.query(sql, params, posicaoMapper());
    }

    @Override
    public List<CurvaABCItem> curvaAbc(UUID filialId, int page, int size) {
        String sql = """
                SELECT mv.filial_id, mv.insumo_id,
                       i.codigo AS insumo_codigo,
                       i.nome   AS insumo_nome,
                       mv.quantidade_total,
                       mv.valor_total,
                       mv.percentual_acumulado,
                       mv.classe
                FROM reporting.mv_curva_abc mv
                JOIN insumos i ON i.id = mv.insumo_id
                WHERE (CAST(:filialId AS uuid) IS NULL OR mv.filial_id = CAST(:filialId AS uuid))
                ORDER BY mv.filial_id, mv.valor_total DESC, i.nome
                LIMIT :size OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("filialId", filialId)
                .addValue("size", size)
                .addValue("offset", offset(page, size));
        return jdbc.query(sql, params, curvaAbcMapper());
    }

    @Override
    public List<RupturaIminenteItem> rupturaIminente(UUID filialId, int page, int size) {
        String sql = """
                SELECT mv.filial_id, mv.insumo_id,
                       i.codigo AS insumo_codigo,
                       i.nome   AS insumo_nome,
                       mv.saldo_total, mv.estoque_minimo, mv.ponto_pedido, mv.situacao
                FROM reporting.mv_ruptura_iminente mv
                JOIN insumos i ON i.id = mv.insumo_id
                WHERE (CAST(:filialId AS uuid) IS NULL OR mv.filial_id = CAST(:filialId AS uuid))
                  AND mv.situacao <> 'NORMAL'
                ORDER BY CASE mv.situacao
                            WHEN 'RUPTURA_TOTAL'        THEN 1
                            WHEN 'ABAIXO_PONTO_PEDIDO'  THEN 2
                            WHEN 'ABAIXO_MINIMO'        THEN 3
                            ELSE 4
                         END, i.nome
                LIMIT :size OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("filialId", filialId)
                .addValue("size", size)
                .addValue("offset", offset(page, size));
        return jdbc.query(sql, params, rupturaMapper());
    }

    @Override
    public List<VencimentoProximoItem> vencimentoProximo(UUID filialId, int diasJanela, int page, int size) {
        LocalDate hoje = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate limite = hoje.plusDays(diasJanela);
        String sql = """
                SELECT sl.filial_id,
                       l.insumo_id,
                       l.id   AS lote_id,
                       i.codigo AS insumo_codigo,
                       i.nome   AS insumo_nome,
                       l.numero_lote,
                       l.data_validade,
                       (l.data_validade - :hoje) AS dias_para_vencer,
                       sl.quantidade_base        AS saldo,
                       l.valor_unitario
                FROM lotes l
                JOIN saldos_lotes sl ON sl.lote_id = l.id
                JOIN insumos i       ON i.id      = l.insumo_id
                WHERE l.data_validade IS NOT NULL
                  AND l.data_validade <= :limite
                  AND sl.quantidade_base > 0
                  AND (CAST(:filialId AS uuid) IS NULL OR sl.filial_id = CAST(:filialId AS uuid))
                ORDER BY l.data_validade ASC, i.nome
                LIMIT :size OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("filialId", filialId)
                .addValue("hoje", Date.valueOf(hoje))
                .addValue("limite", Date.valueOf(limite))
                .addValue("size", size)
                .addValue("offset", offset(page, size));
        return jdbc.query(sql, params, vencimentoMapper());
    }

    @Override
    public List<MovimentacaoPorPeriodoItem> movimentacaoPorPeriodo(
            UUID filialId, PeriodoFiltro periodo, String tipoMovimentacao, int page, int size) {
        String sql = """
                SELECT m.filial_id,
                       im.insumo_id,
                       i.codigo AS insumo_codigo,
                       i.nome   AS insumo_nome,
                       m.tipo   AS tipo_movimentacao,
                       COUNT(DISTINCT m.id)                          AS qtd_movs,
                       SUM(im.quantidade_base)                       AS qtd_total,
                       SUM(im.quantidade_base * im.valor_unitario)   AS valor_total
                FROM movimentacoes m
                JOIN items_movimentacao im ON im.movimentacao_id = m.id
                JOIN insumos i             ON i.id               = im.insumo_id
                WHERE m.data_movimentacao >= :inicio
                  AND m.data_movimentacao <= :fim
                  AND (CAST(:filialId AS uuid)    IS NULL OR m.filial_id = CAST(:filialId AS uuid))
                  AND (CAST(:tipo AS varchar)     IS NULL OR m.tipo      = CAST(:tipo AS varchar))
                GROUP BY m.filial_id, im.insumo_id, i.codigo, i.nome, m.tipo
                ORDER BY m.filial_id, i.nome, m.tipo
                LIMIT :size OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("filialId", filialId)
                .addValue("inicio", Timestamp.from(periodo.inicio()))
                .addValue("fim", Timestamp.from(periodo.fim()))
                .addValue("tipo", tipoMovimentacao)
                .addValue("size", size)
                .addValue("offset", offset(page, size));
        return jdbc.query(sql, params, movimentacaoMapper());
    }

    @Override
    public List<DivergenciaInventarioItem> divergenciaInventario(
            UUID filialId, PeriodoFiltro periodo, int page, int size) {
        String sql = """
                SELECT a.filial_id,
                       a.insumo_id,
                       i.codigo AS insumo_codigo,
                       i.nome   AS insumo_nome,
                       COUNT(*)                                                                  AS qtd_ajustes,
                       SUM(CASE WHEN a.quantidade_diff > 0 THEN  a.quantidade_diff ELSE 0 END)   AS diff_positiva,
                       SUM(CASE WHEN a.quantidade_diff < 0 THEN -a.quantidade_diff ELSE 0 END)   AS diff_negativa,
                       SUM(a.quantidade_diff)                                                    AS diff_liquida
                FROM ajustes_estoque a
                JOIN insumos i ON i.id = a.insumo_id
                WHERE a.status = 'APROVADO'
                  AND a.data_aprovacao >= :inicio
                  AND a.data_aprovacao <= :fim
                  AND (CAST(:filialId AS uuid) IS NULL OR a.filial_id = CAST(:filialId AS uuid))
                GROUP BY a.filial_id, a.insumo_id, i.codigo, i.nome
                ORDER BY ABS(SUM(a.quantidade_diff)) DESC, i.nome
                LIMIT :size OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("filialId", filialId)
                .addValue("inicio", Timestamp.from(periodo.inicio()))
                .addValue("fim", Timestamp.from(periodo.fim()))
                .addValue("size", size)
                .addValue("offset", offset(page, size));
        return jdbc.query(sql, params, divergenciaMapper());
    }

    @Override
    public void refreshViewsMaterializadas() {
        // CONCURRENTLY exige UNIQUE INDEX e MV populada ao menos uma vez —
        // ambos garantidos pela V009 (CREATE MATERIALIZED VIEW ... WITH DATA).
        try {
            jdbc.getJdbcTemplate().execute("REFRESH MATERIALIZED VIEW CONCURRENTLY reporting.mv_curva_abc");
            jdbc.getJdbcTemplate().execute("REFRESH MATERIALIZED VIEW CONCURRENTLY reporting.mv_ruptura_iminente");
        } catch (EmptyResultDataAccessException ignored) {
            // não-aplicável a DDL, mantido por simetria caso a query mude.
        }
    }

    private static int offset(int page, int size) {
        return Math.max(0, page) * Math.max(1, size);
    }

    // -------- mappers --------

    private static RowMapper<PosicaoEstoqueItem> posicaoMapper() {
        return (rs, i) -> new PosicaoEstoqueItem(
                uuid(rs, "filial_id"),
                uuid(rs, "insumo_id"),
                rs.getString("insumo_codigo"),
                rs.getString("insumo_nome"),
                rs.getBigDecimal("saldo_total"),
                rs.getBigDecimal("valor_estoque"),
                rs.getLong("quantidade_lotes"));
    }

    private static RowMapper<CurvaABCItem> curvaAbcMapper() {
        return (rs, i) -> new CurvaABCItem(
                uuid(rs, "filial_id"),
                uuid(rs, "insumo_id"),
                rs.getString("insumo_codigo"),
                rs.getString("insumo_nome"),
                rs.getBigDecimal("quantidade_total"),
                rs.getBigDecimal("valor_total"),
                rs.getBigDecimal("percentual_acumulado"),
                ClasseABC.valueOf(rs.getString("classe")));
    }

    private static RowMapper<RupturaIminenteItem> rupturaMapper() {
        return (rs, i) -> new RupturaIminenteItem(
                uuid(rs, "filial_id"),
                uuid(rs, "insumo_id"),
                rs.getString("insumo_codigo"),
                rs.getString("insumo_nome"),
                rs.getBigDecimal("saldo_total"),
                rs.getBigDecimal("estoque_minimo"),
                rs.getBigDecimal("ponto_pedido"),
                SituacaoRuptura.valueOf(rs.getString("situacao")));
    }

    private static RowMapper<VencimentoProximoItem> vencimentoMapper() {
        return (rs, i) -> new VencimentoProximoItem(
                uuid(rs, "filial_id"),
                uuid(rs, "insumo_id"),
                uuid(rs, "lote_id"),
                rs.getString("insumo_codigo"),
                rs.getString("insumo_nome"),
                rs.getString("numero_lote"),
                rs.getObject("data_validade", LocalDate.class),
                rs.getLong("dias_para_vencer"),
                rs.getBigDecimal("saldo"),
                rs.getBigDecimal("valor_unitario"));
    }

    private static RowMapper<MovimentacaoPorPeriodoItem> movimentacaoMapper() {
        return (rs, i) -> new MovimentacaoPorPeriodoItem(
                uuid(rs, "filial_id"),
                uuid(rs, "insumo_id"),
                rs.getString("insumo_codigo"),
                rs.getString("insumo_nome"),
                rs.getString("tipo_movimentacao"),
                rs.getLong("qtd_movs"),
                nullSafeBigDecimal(rs, "qtd_total"),
                nullSafeBigDecimal(rs, "valor_total"));
    }

    private static RowMapper<DivergenciaInventarioItem> divergenciaMapper() {
        return (rs, i) -> new DivergenciaInventarioItem(
                uuid(rs, "filial_id"),
                uuid(rs, "insumo_id"),
                rs.getString("insumo_codigo"),
                rs.getString("insumo_nome"),
                rs.getLong("qtd_ajustes"),
                nullSafeBigDecimal(rs, "diff_positiva"),
                nullSafeBigDecimal(rs, "diff_negativa"),
                nullSafeBigDecimal(rs, "diff_liquida"));
    }

    private static UUID uuid(ResultSet rs, String col) throws SQLException {
        Object obj = rs.getObject(col);
        if (obj == null) return null;
        if (obj instanceof UUID u) return u;
        return UUID.fromString(obj.toString());
    }

    private static BigDecimal nullSafeBigDecimal(ResultSet rs, String col) throws SQLException {
        BigDecimal v = rs.getBigDecimal(col);
        return v != null ? v : BigDecimal.ZERO;
    }
}
