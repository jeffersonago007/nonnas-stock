package com.nonnas.operations.application.depara;

import com.nonnas.operations.application.ports.FornecedorInsumoDeParaRepository;
import com.nonnas.operations.domain.FornecedorInsumoDePara;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lista mapeamentos (cProd → insumo) aprendidos de um fornecedor, enriquecidos
 * com nome/código do insumo via SQL nativo cross-context (ADR 0010). Usado pela
 * UI admin pra operador identificar e remover mapeamentos errados.
 */
@Service
public class ListarDeParasUseCase {

    private final FornecedorInsumoDeParaRepository repo;
    private final NamedParameterJdbcTemplate jdbc;

    public ListarDeParasUseCase(FornecedorInsumoDeParaRepository repo,
                                NamedParameterJdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<DeParaItem> execute(UUID fornecedorId) {
        List<FornecedorInsumoDePara> deparas = repo.findByFornecedor(fornecedorId);
        if (deparas.isEmpty()) return List.of();

        List<UUID> insumoIds = deparas.stream().map(FornecedorInsumoDePara::insumoId).toList();
        Map<UUID, InsumoBasico> meta = fetchInsumosMeta(insumoIds);

        List<DeParaItem> out = new ArrayList<>(deparas.size());
        for (var dp : deparas) {
            InsumoBasico ins = meta.get(dp.insumoId());
            out.add(new DeParaItem(
                    dp.codigoFornecedor(),
                    dp.insumoId(),
                    ins == null ? null : ins.codigo(),
                    ins == null ? "(insumo removido)" : ins.nome(),
                    dp.createdAt(),
                    dp.lastUsedAt()));
        }
        return out;
    }

    private Map<UUID, InsumoBasico> fetchInsumosMeta(List<UUID> insumoIds) {
        String sql = "SELECT id, codigo, nome FROM insumos WHERE id IN (:ids)";
        Map<UUID, InsumoBasico> out = new HashMap<>();
        jdbc.query(sql, new MapSqlParameterSource("ids", insumoIds), rs -> {
            UUID id = rs.getObject("id", UUID.class);
            out.put(id, new InsumoBasico(rs.getString("codigo"), rs.getString("nome")));
        });
        return out;
    }

    public record DeParaItem(
            String codigoFornecedor,
            UUID insumoId,
            String insumoCodigo,
            String insumoNome,
            Instant createdAt,
            Instant lastUsedAt
    ) {}

    private record InsumoBasico(String codigo, String nome) {}
}
