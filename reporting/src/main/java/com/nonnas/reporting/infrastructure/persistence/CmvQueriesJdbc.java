package com.nonnas.reporting.infrastructure.persistence;

import com.nonnas.reporting.application.ports.CmvQueries;
import com.nonnas.reporting.domain.CmvPorCanalItem;
import com.nonnas.reporting.domain.CmvPorInsumoItem;
import com.nonnas.reporting.domain.CmvPorProdutoItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SQL nativo cross-context (ADR 0010) para as 3 perspectivas de CMV.
 * Sem JPA: não importamos entities de outros bounded contexts.
 *
 * <p>Janela de tempo usa {@code data_movimentacao} em {@code movimentacoes}
 * (snapshot do momento da saída) — não usar {@code created_at} para evitar
 * confusão com lançamentos retroativos eventualmente permitidos.
 */
@Repository
public class CmvQueriesJdbc implements CmvQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public CmvQueriesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CmvPorInsumoItem> cmvPorInsumo(Instant de, Instant ate, UUID filialId) {
        String sql = """
                SELECT i.id                                                AS insumo_id,
                       i.codigo                                            AS codigo,
                       i.nome                                              AS nome,
                       SUM(im.quantidade_base)                             AS qtd_vendida,
                       SUM(im.quantidade_base * im.valor_unitario)         AS cmv_total,
                       CASE WHEN SUM(im.quantidade_base) > 0
                            THEN SUM(im.quantidade_base * im.valor_unitario) / SUM(im.quantidade_base)
                            ELSE 0 END                                     AS custo_medio_periodo,
                       COUNT(DISTINCT m.id)                                AS qtd_movs
                FROM items_movimentacao im
                JOIN movimentacoes m ON m.id = im.movimentacao_id
                JOIN insumos i       ON i.id = im.insumo_id
                WHERE m.tipo = 'SAIDA_VENDA'
                  AND m.data_movimentacao >= :de
                  AND m.data_movimentacao <  :ate
                  AND (CAST(:filialId AS uuid) IS NULL OR m.filial_id = CAST(:filialId AS uuid))
                GROUP BY i.id, i.codigo, i.nome
                ORDER BY cmv_total DESC
                """;
        var params = baseParams(de, ate, filialId);
        return jdbc.query(sql, params, (rs, n) -> new CmvPorInsumoItem(
                UUID.fromString(rs.getString("insumo_id")),
                rs.getString("codigo"),
                rs.getString("nome"),
                rs.getBigDecimal("qtd_vendida"),
                rs.getBigDecimal("cmv_total"),
                rs.getBigDecimal("custo_medio_periodo"),
                rs.getLong("qtd_movs")));
    }

    @Override
    public List<CmvPorProdutoItem> cmvPorProduto(Instant de, Instant ate, UUID filialId) {
        // Une 2 caminhos: documento_origem_tipo='FICHA_TECNICA' (resolve ficha →
        // produto) e 'PRODUTO_REVENDA' (id já é o produto). LEFT JOINs + COALESCE
        // pra colapsar; filtro elimina movimentações que não casaram (origens
        // inesperadas — ajustes, perdas — não devem ser SAIDA_VENDA, mas
        // defendemos).
        String sql = """
                SELECT pv.id                                          AS produto_id,
                       pv.codigo                                      AS codigo,
                       pv.nome                                        AS nome,
                       SUM(im.quantidade_base * im.valor_unitario)    AS cmv_total,
                       SUM(im.quantidade_base)                        AS qtd_vendida,
                       COUNT(DISTINCT m.id)                           AS qtd_movs
                FROM movimentacoes m
                JOIN items_movimentacao im ON im.movimentacao_id = m.id
                LEFT JOIN fichas_tecnicas ft
                       ON m.documento_origem_tipo = 'FICHA_TECNICA' AND ft.id = m.documento_origem_id
                JOIN produtos_vendaveis pv
                       ON pv.id = COALESCE(
                              ft.produto_vendavel_id,
                              CASE WHEN m.documento_origem_tipo = 'PRODUTO_REVENDA' THEN m.documento_origem_id END
                          )
                WHERE m.tipo = 'SAIDA_VENDA'
                  AND m.data_movimentacao >= :de
                  AND m.data_movimentacao <  :ate
                  AND (CAST(:filialId AS uuid) IS NULL OR m.filial_id = CAST(:filialId AS uuid))
                GROUP BY pv.id, pv.codigo, pv.nome
                ORDER BY cmv_total DESC
                """;
        var params = baseParams(de, ate, filialId);
        return jdbc.query(sql, params, (rs, n) -> new CmvPorProdutoItem(
                UUID.fromString(rs.getString("produto_id")),
                rs.getString("codigo"),
                rs.getString("nome"),
                rs.getBigDecimal("qtd_vendida"),
                rs.getBigDecimal("cmv_total"),
                rs.getLong("qtd_movs")));
    }

    @Override
    public List<CmvPorCanalItem> cmvPorCanal(Instant de, Instant ate, UUID filialId) {
        // Receita líquida = pedidos_canais.valor_liquido (T-CANAL-06) somada
        // por canal. CMV = soma do custo dos itens movimentados pela
        // movimentacao vinculada ao pedido. Margem bruta = receita − CMV
        // (antes de comissão de marketplace).
        // SUM(valor_liquido) na subquery pra evitar duplicação por JOIN com items_movimentacao.
        String sql = """
                WITH cmv_por_movimentacao AS (
                    SELECT m.id AS mov_id,
                           SUM(im.quantidade_base * im.valor_unitario) AS cmv
                    FROM movimentacoes m
                    JOIN items_movimentacao im ON im.movimentacao_id = m.id
                    WHERE m.tipo = 'SAIDA_VENDA'
                      AND m.data_movimentacao >= :de
                      AND m.data_movimentacao <  :ate
                      AND (CAST(:filialId AS uuid) IS NULL OR m.filial_id = CAST(:filialId AS uuid))
                    GROUP BY m.id
                )
                SELECT pc.canal_tipo                          AS canal,
                       COUNT(DISTINCT pc.id)                  AS qtd_pedidos,
                       SUM(pc.valor_liquido)                  AS receita_liquida_total,
                       SUM(c.cmv)                             AS cmv_total
                FROM pedidos_canais pc
                JOIN cmv_por_movimentacao c ON c.mov_id = pc.movimentacao_id
                WHERE pc.movimentacao_id IS NOT NULL
                  AND (CAST(:filialId AS uuid) IS NULL OR pc.filial_id = CAST(:filialId AS uuid))
                GROUP BY pc.canal_tipo
                ORDER BY cmv_total DESC
                """;
        var params = baseParams(de, ate, filialId);
        return jdbc.query(sql, params, (rs, n) -> {
            BigDecimal receita = rs.getBigDecimal("receita_liquida_total");
            BigDecimal cmv = rs.getBigDecimal("cmv_total");
            BigDecimal margem = (receita != null && cmv != null) ? receita.subtract(cmv) : null;
            return new CmvPorCanalItem(
                    rs.getString("canal"),
                    rs.getLong("qtd_pedidos"),
                    receita,
                    cmv,
                    margem);
        });
    }

    private MapSqlParameterSource baseParams(Instant de, Instant ate, UUID filialId) {
        return new MapSqlParameterSource()
                .addValue("de", Timestamp.from(de))
                .addValue("ate", Timestamp.from(ate))
                .addValue("filialId", filialId != null ? filialId.toString() : null);
    }
}
