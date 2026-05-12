package com.nonnas.reporting.testsupport;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Insere dados nas tabelas de catalog/inventory-core/operations diretamente
 * via SQL para os ITs de reporting. Reporting consome essas tabelas em
 * leitura — não importa as classes Java desses módulos.
 */
@Component
public class ReportingFixtures {

    private final NamedParameterJdbcTemplate jdbc;

    public ReportingFixtures(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID criarCategoria(String nome) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO categorias_insumo(id, nome, ativa, created_at, updated_at)
                VALUES (:id, :nome, TRUE, NOW(), NOW())
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("nome", nome));
        return id;
    }

    /** Reusa uma das unidades já cadastradas pelo seed V004 (G, KG, ML, L, UN, CX, PORCAO). */
    public UUID idUnidadePadrao(String codigo) {
        return jdbc.queryForObject("SELECT id FROM unidades_medida WHERE codigo = :c",
                new MapSqlParameterSource("c", codigo), UUID.class);
    }

    public UUID criarInsumo(UUID categoriaId, UUID unidadeId, String codigo, String nome) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO insumos(id, codigo, nome, categoria_id, unidade_base_id,
                                    controla_lote, controla_validade, ativo, created_at, updated_at)
                VALUES (:id, :codigo, :nome, :catId, :unId, TRUE, TRUE, TRUE, NOW(), NOW())
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("codigo", codigo).addValue("nome", nome)
                .addValue("catId", categoriaId).addValue("unId", unidadeId));
        return id;
    }

    public void criarInsumoFilial(UUID insumoId, UUID filialId,
                                  BigDecimal estoqueMinimo, BigDecimal pontoPedido) {
        jdbc.update("""
                INSERT INTO insumos_filiais(id, insumo_id, filial_id, estoque_minimo,
                                            ponto_pedido, ativo, created_at, updated_at)
                VALUES (gen_random_uuid(), :ins, :fil, :min, :pp, TRUE, NOW(), NOW())
                """, new MapSqlParameterSource()
                .addValue("ins", insumoId).addValue("fil", filialId)
                .addValue("min", estoqueMinimo).addValue("pp", pontoPedido));
    }

    public UUID criarLote(UUID insumoId, String numero, LocalDate validade, BigDecimal valorUnitario) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO lotes(id, insumo_id, numero_lote, data_validade,
                                  valor_unitario, created_at)
                VALUES (:id, :ins, :num, :val, :vu, NOW())
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("ins", insumoId).addValue("num", numero)
                .addValue("val", validade != null ? Date.valueOf(validade) : null)
                .addValue("vu", valorUnitario));
        return id;
    }

    public void criarSaldo(UUID loteId, UUID filialId, BigDecimal quantidade) {
        jdbc.update("""
                INSERT INTO saldos_lotes(lote_id, filial_id, quantidade_base, atualizado_em)
                VALUES (:lote, :fil, :qtd, NOW())
                ON CONFLICT (lote_id, filial_id) DO UPDATE SET quantidade_base = :qtd
                """, new MapSqlParameterSource()
                .addValue("lote", loteId).addValue("fil", filialId).addValue("qtd", quantidade));
    }

    public UUID criarMovimentacao(UUID filialId, UUID usuarioId, String tipo, Instant data) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO movimentacoes(id, filial_id, usuario_id, tipo, data_movimentacao,
                                          gerou_negativo, created_at)
                VALUES (:id, :fil, :user, :tipo, :data, FALSE, NOW())
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("fil", filialId).addValue("user", usuarioId)
                .addValue("tipo", tipo).addValue("data", Timestamp.from(data)));
        return id;
    }

    public void criarItemMovimentacao(UUID movId, UUID insumoId, UUID loteId, UUID unidadeId,
                                      BigDecimal quantidadeBase, BigDecimal valorUnitario) {
        jdbc.update("""
                INSERT INTO items_movimentacao(id, movimentacao_id, insumo_id, lote_id,
                                               unidade_lancamento_id, quantidade_lancada,
                                               quantidade_base, valor_unitario)
                VALUES (gen_random_uuid(), :mov, :ins, :lote, :un, :qtd, :qtd, :vu)
                """, new MapSqlParameterSource()
                .addValue("mov", movId).addValue("ins", insumoId).addValue("lote", loteId)
                .addValue("un", unidadeId).addValue("qtd", quantidadeBase).addValue("vu", valorUnitario));
    }

    public UUID criarAjusteAprovado(UUID filialId, UUID insumoId, UUID unidadeId,
                                    BigDecimal diff, Instant dataAprovacao) {
        UUID id = UUID.randomUUID();
        UUID solicitante = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ajustes_estoque(id, filial_id, insumo_id, unidade_id, quantidade_diff,
                                            motivo, status, requer_aprovacao,
                                            solicitado_por, aprovado_por,
                                            data_solicitacao, data_aprovacao,
                                            created_at, updated_at)
                VALUES (:id, :fil, :ins, :un, :diff,
                        'Ajuste de teste', 'APROVADO', TRUE,
                        :solic, :solic,
                        :data, :data,
                        NOW(), NOW())
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("fil", filialId).addValue("ins", insumoId)
                .addValue("un", unidadeId).addValue("diff", diff)
                .addValue("solic", solicitante)
                .addValue("data", Timestamp.from(dataAprovacao)));
        return id;
    }

    /**
     * Cria N lotes para um insumo na filial, com saldo unitário em cada um.
     * Usa generate_series para inserir em uma única ida ao banco — usado nos
     * testes de performance.
     */
    public void popularLotesEmLote(int n, UUID insumoId, UUID filialId, BigDecimal valorUnitario) {
        String prefixo = "PERF-" + UUID.randomUUID().toString().substring(0, 8) + "-";
        jdbc.update("""
                INSERT INTO lotes(id, insumo_id, numero_lote, data_validade,
                                  valor_unitario, created_at)
                SELECT gen_random_uuid(), :ins, :prefixo || s,
                       CURRENT_DATE + (s % 365),
                       :vu, NOW()
                FROM generate_series(1, :n) s
                """, new MapSqlParameterSource()
                .addValue("ins", insumoId)
                .addValue("prefixo", prefixo)
                .addValue("vu", valorUnitario)
                .addValue("n", n));
        jdbc.update("""
                INSERT INTO saldos_lotes(lote_id, filial_id, quantidade_base, atualizado_em)
                SELECT id, :fil, 1.0, NOW()
                FROM lotes WHERE numero_lote LIKE :pat
                """, new MapSqlParameterSource()
                .addValue("fil", filialId)
                .addValue("pat", prefixo + "%"));
    }

    /**
     * Cria N movimentações de saída no período (últimos 60 dias) cada uma com
     * 1 item. Usado nos testes de performance para queries cross-tabela pesadas.
     */
    public void popularMovimentacoesEmLote(int n, UUID filialId, UUID usuarioId,
                                           UUID insumoId, UUID loteId, UUID unidadeId,
                                           BigDecimal valorUnitario) {
        String tag = "PERF-MOV-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO movimentacoes(id, filial_id, usuario_id, tipo, data_movimentacao,
                                          observacao, gerou_negativo, created_at)
                SELECT gen_random_uuid(), :fil, :user, 'SAIDA_VENDA',
                       NOW() - ((s % 60) || ' days')::interval,
                       :tag, FALSE, NOW()
                FROM generate_series(1, :n) s
                """, new MapSqlParameterSource()
                .addValue("fil", filialId)
                .addValue("user", usuarioId)
                .addValue("tag", tag)
                .addValue("n", n));
        jdbc.update("""
                INSERT INTO items_movimentacao(id, movimentacao_id, insumo_id, lote_id,
                                               unidade_lancamento_id, quantidade_lancada,
                                               quantidade_base, valor_unitario)
                SELECT gen_random_uuid(), m.id, :ins, :lote, :un, 1.0, 1.0, :vu
                FROM movimentacoes m WHERE m.observacao = :tag
                """, new MapSqlParameterSource()
                .addValue("ins", insumoId)
                .addValue("lote", loteId)
                .addValue("un", unidadeId)
                .addValue("tag", tag)
                .addValue("vu", valorUnitario));
    }

    public void refreshViewsMaterializadas() {
        jdbc.getJdbcTemplate().execute("REFRESH MATERIALIZED VIEW reporting.mv_curva_abc");
        // mv_ruptura_iminente virou view comum em V023 — não precisa refresh.
    }

    public void limparTudo() {
        // ordem respeita FKs; preserva unidades do seed V004
        jdbc.getJdbcTemplate().update("DELETE FROM ajustes_estoque");
        jdbc.getJdbcTemplate().update("DELETE FROM items_movimentacao");
        jdbc.getJdbcTemplate().update("DELETE FROM movimentacoes");
        jdbc.getJdbcTemplate().update("DELETE FROM saldos_lotes");
        jdbc.getJdbcTemplate().update("DELETE FROM lotes");
        jdbc.getJdbcTemplate().update("DELETE FROM insumos_filiais");
        jdbc.getJdbcTemplate().update("DELETE FROM insumos");
        jdbc.getJdbcTemplate().update("DELETE FROM categorias_insumo");
        refreshViewsMaterializadas();
    }
}
