package com.nonnas.operations.application.depara;

import com.nonnas.operations.application.ports.FornecedorInsumoDeParaRepository;
import com.nonnas.operations.domain.FornecedorInsumoDePara;
import com.nonnas.operations.testsupport.AbstractOperationsIntegrationTest;
import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobre o CRUD de de-para usado pela tela admin de Fornecedores → Mapeamentos.
 */
class DeParaIT extends AbstractOperationsIntegrationTest {

    @Autowired private ListarDeParasUseCase listar;
    @Autowired private ApagarDeParaUseCase apagar;
    @Autowired private FornecedorInsumoDeParaRepository repo;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    @Test
    void listar_enriqueceComNomeCodigoDoInsumo() {
        UUID fornecedor = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID categoria = UUID.randomUUID();
        UUID unidade = unidadePorCodigo("UN");
        inserirCategoria(categoria, "Bebidas");
        inserirInsumo(insumo, "COD-X", "Item de Teste", categoria, unidade);

        repo.save(FornecedorInsumoDePara.novo(fornecedor, "AAA-001", insumo, Instant.now()));

        List<ListarDeParasUseCase.DeParaItem> itens = listar.execute(fornecedor);
        assertThat(itens).hasSize(1);
        assertThat(itens.get(0).codigoFornecedor()).isEqualTo("AAA-001");
        assertThat(itens.get(0).insumoCodigo()).isEqualTo("COD-X");
        assertThat(itens.get(0).insumoNome()).isEqualTo("Item de Teste");
    }

    @Test
    void listar_quandoVazio_retornaListaVazia() {
        assertThat(listar.execute(UUID.randomUUID())).isEmpty();
    }

    @Test
    void apagar_removeMapeamento() {
        UUID fornecedor = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID categoria = UUID.randomUUID();
        UUID unidade = unidadePorCodigo("UN");
        inserirCategoria(categoria, "Bebidas");
        inserirInsumo(insumo, "COD-Y", "Outro", categoria, unidade);

        repo.save(FornecedorInsumoDePara.novo(fornecedor, "BBB-001", insumo, Instant.now()));
        assertThat(repo.findByFornecedorAndCodigo(fornecedor, "BBB-001")).isPresent();

        apagar.execute(fornecedor, "BBB-001");

        assertThat(repo.findByFornecedorAndCodigo(fornecedor, "BBB-001")).isEmpty();
    }

    @Test
    void apagar_codigoVazio_rejeita() {
        UUID fornecedor = UUID.randomUUID();
        assertThatThrownBy(() -> apagar.execute(fornecedor, ""))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> apagar.execute(fornecedor, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void listar_insumoOrfaoNoDB_indica_insumoRemovido() {
        UUID fornecedor = UUID.randomUUID();
        UUID insumoInexistente = UUID.randomUUID();
        repo.save(FornecedorInsumoDePara.novo(fornecedor, "CCC-001", insumoInexistente, Instant.now()));

        List<ListarDeParasUseCase.DeParaItem> itens = listar.execute(fornecedor);
        assertThat(itens).hasSize(1);
        assertThat(itens.get(0).insumoNome()).isEqualTo("(insumo removido)");
        assertThat(itens.get(0).insumoCodigo()).isNull();
    }

    private void inserirCategoria(UUID id, String nome) {
        jdbc.update("""
                INSERT INTO categorias_insumo (id, nome, ativa, created_at, updated_at)
                VALUES (:id, :nome, TRUE, :now, :now)
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("nome", nome)
                        .addValue("now", java.sql.Timestamp.from(Instant.now())));
    }

    private UUID unidadePorCodigo(String codigo) {
        return jdbc.queryForObject(
                "SELECT id FROM unidades_medida WHERE codigo = :codigo",
                new MapSqlParameterSource("codigo", codigo),
                UUID.class);
    }

    private void inserirInsumo(UUID id, String codigo, String nome, UUID categoriaId, UUID unidadeBaseId) {
        jdbc.update("""
                INSERT INTO insumos (id, codigo, nome, categoria_id, unidade_base_id,
                                     controla_lote, controla_validade, ativo, created_at, updated_at)
                VALUES (:id, :codigo, :nome, :catId, :uniId, TRUE, TRUE, TRUE, :now, :now)
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("codigo", codigo)
                        .addValue("nome", nome)
                        .addValue("catId", categoriaId)
                        .addValue("uniId", unidadeBaseId)
                        .addValue("now", java.sql.Timestamp.from(Instant.now())));
    }
}
