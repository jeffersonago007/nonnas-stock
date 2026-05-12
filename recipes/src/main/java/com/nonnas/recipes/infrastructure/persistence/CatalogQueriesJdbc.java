package com.nonnas.recipes.infrastructure.persistence;

import com.nonnas.recipes.application.ports.CatalogQueries;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
class CatalogQueriesJdbc implements CatalogQueries {

    private final NamedParameterJdbcTemplate jdbc;

    CatalogQueriesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> findUnidadeBaseDoInsumo(UUID insumoId) {
        String sql = "SELECT unidade_base_id FROM insumos WHERE id = :id";
        try {
            UUID uid = jdbc.queryForObject(sql, new MapSqlParameterSource("id", insumoId),
                    (rs, rn) -> rs.getObject("unidade_base_id", UUID.class));
            return Optional.ofNullable(uid);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<InsumoComSaldo> findInsumoComSaldo(UUID insumoId, UUID filialId) {
        String sql = """
                SELECT i.id AS insumo_id, i.codigo, i.nome,
                       u.id AS unidade_id, u.codigo AS unidade_codigo,
                       COALESCE((
                           SELECT SUM(sl.quantidade_base)
                           FROM saldos_lotes sl
                           JOIN lotes l ON l.id = sl.lote_id
                           WHERE l.insumo_id = i.id AND sl.filial_id = :filialId
                       ), 0) AS saldo
                FROM insumos i
                JOIN unidades_medida u ON u.id = i.unidade_base_id
                WHERE i.id = :insumoId
                """;
        try {
            InsumoComSaldo r = jdbc.queryForObject(sql,
                    new MapSqlParameterSource()
                            .addValue("insumoId", insumoId)
                            .addValue("filialId", filialId),
                    (rs, rn) -> new InsumoComSaldo(
                            rs.getObject("insumo_id", UUID.class),
                            rs.getString("codigo"),
                            rs.getString("nome"),
                            rs.getObject("unidade_id", UUID.class),
                            rs.getString("unidade_codigo"),
                            rs.getBigDecimal("saldo")));
            return Optional.ofNullable(r);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<InsumoComSaldo> findInsumosOrfaosComSaldo(UUID filialId, Set<UUID> insumosJaVinculados) {
        // SUM(saldos_lotes) por insumo na filial, só ativos, só com saldo positivo,
        // excluindo os que já têm produto REVENDA vinculado.
        StringBuilder sql = new StringBuilder("""
                SELECT i.id AS insumo_id, i.codigo, i.nome,
                       u.id AS unidade_id, u.codigo AS unidade_codigo,
                       saldo.total AS saldo
                FROM insumos i
                JOIN unidades_medida u ON u.id = i.unidade_base_id
                JOIN (
                    SELECT l.insumo_id, SUM(sl.quantidade_base) AS total
                    FROM saldos_lotes sl
                    JOIN lotes l ON l.id = sl.lote_id
                    WHERE sl.filial_id = :filialId
                    GROUP BY l.insumo_id
                    HAVING SUM(sl.quantidade_base) > 0
                ) saldo ON saldo.insumo_id = i.id
                WHERE i.ativo = TRUE
                """);
        var params = new MapSqlParameterSource().addValue("filialId", filialId);
        if (!insumosJaVinculados.isEmpty()) {
            sql.append("  AND i.id NOT IN (:vinculados)\n");
            params.addValue("vinculados", new ArrayList<>(insumosJaVinculados));
        }
        sql.append("ORDER BY i.nome");
        return jdbc.query(sql.toString(), params,
                (rs, rn) -> new InsumoComSaldo(
                        rs.getObject("insumo_id", UUID.class),
                        rs.getString("codigo"),
                        rs.getString("nome"),
                        rs.getObject("unidade_id", UUID.class),
                        rs.getString("unidade_codigo"),
                        rs.getBigDecimal("saldo")));
    }

    @Override
    public Map<UUID, Integer> contarVendasPorProdutoUltimos30Dias(UUID filialId) {
        String sql = """
                SELECT produto_id, COUNT(*) AS vendas
                FROM (
                    SELECT ft.produto_vendavel_id AS produto_id
                    FROM movimentacoes m
                    JOIN fichas_tecnicas ft ON ft.id = m.documento_origem_id
                    WHERE m.tipo = 'SAIDA_VENDA'
                      AND m.documento_origem_tipo = 'FICHA_TECNICA'
                      AND m.data_movimentacao >= NOW() - INTERVAL '30 days'
                      AND m.filial_id = :filialId
                    UNION ALL
                    SELECT m.documento_origem_id AS produto_id
                    FROM movimentacoes m
                    WHERE m.tipo = 'SAIDA_VENDA'
                      AND m.documento_origem_tipo = 'PRODUTO_REVENDA'
                      AND m.data_movimentacao >= NOW() - INTERVAL '30 days'
                      AND m.filial_id = :filialId
                ) AS vendas
                GROUP BY produto_id
                """;
        Map<UUID, Integer> out = new HashMap<>();
        jdbc.query(sql, new MapSqlParameterSource("filialId", filialId), rs -> {
            out.put(rs.getObject("produto_id", UUID.class), rs.getInt("vendas"));
        });
        return out;
    }
}
