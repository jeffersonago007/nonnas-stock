package com.nonnas.recipes.infrastructure.persistence;

import com.nonnas.recipes.application.ports.PreviewVendaQueries;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class PreviewVendaQueriesJdbc implements PreviewVendaQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public PreviewVendaQueriesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, InsumoMeta> fetchInsumoMeta(Collection<UUID> insumoIds, UUID filialId) {
        if (insumoIds.isEmpty()) return Map.of();
        String sql = """
                SELECT i.id              AS insumo_id,
                       i.nome            AS insumo_nome,
                       u.codigo          AS unidade_codigo,
                       i.controla_validade AS controla_validade,
                       COALESCE((
                           SELECT SUM(sl.quantidade_base)
                           FROM saldos_lotes sl
                           JOIN lotes l ON l.id = sl.lote_id
                           WHERE l.insumo_id = i.id AND sl.filial_id = :filialId
                       ), 0) AS saldo_atual
                FROM insumos i
                JOIN unidades_medida u ON u.id = i.unidade_base_id
                WHERE i.id IN (:insumoIds)
                """;
        var params = new MapSqlParameterSource()
                .addValue("insumoIds", insumoIds)
                .addValue("filialId", filialId);
        Map<UUID, InsumoMeta> out = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            UUID id = rs.getObject("insumo_id", UUID.class);
            out.put(id, new InsumoMeta(
                    rs.getString("insumo_nome"),
                    rs.getString("unidade_codigo"),
                    rs.getBoolean("controla_validade"),
                    rs.getBigDecimal("saldo_atual")
            ));
        });
        return out;
    }

    @Override
    public Map<UUID, LoteMeta> fetchLoteMeta(Collection<UUID> loteIds) {
        if (loteIds.isEmpty()) return Map.of();
        String sql = """
                SELECT id, numero_lote, data_validade
                FROM lotes
                WHERE id IN (:loteIds)
                """;
        var params = new MapSqlParameterSource("loteIds", loteIds);
        Map<UUID, LoteMeta> out = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            UUID id = rs.getObject("id", UUID.class);
            Date d = rs.getDate("data_validade");
            out.put(id, new LoteMeta(
                    rs.getString("numero_lote"),
                    d == null ? null : d.toLocalDate()
            ));
        });
        return out;
    }
}
