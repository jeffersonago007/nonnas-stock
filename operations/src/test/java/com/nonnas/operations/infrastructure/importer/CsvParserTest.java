package com.nonnas.operations.infrastructure.importer;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void csv_virgula_validParseaTodasLinhas() {
        String csv = """
                insumo_id,unidade_id,numero_lote,quantidade,valor_unitario,data_fabricacao,data_validade
                11111111-1111-1111-1111-111111111111,22222222-2222-2222-2222-222222222222,L-001,10,25.5,2026-04-01,2026-09-01
                33333333-3333-3333-3333-333333333333,22222222-2222-2222-2222-222222222222,L-002,5.25,18.0,,2026-12-01
                """;
        var plan = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "carga.csv");

        assertThat(plan.linhas()).hasSize(2);
        assertThat(plan.hashSha256()).hasSize(64);
        assertThat(plan.linhas().get(0).numeroLote()).isEqualTo("L-001");
        assertThat(plan.linhas().get(0).quantidade()).isEqualByComparingTo("10");
        assertThat(plan.linhas().get(1).quantidade()).isEqualByComparingTo("5.25");
        assertThat(plan.linhas().get(1).dataFabricacao()).isNull();
    }

    @Test
    void csv_pontoVirgula_detectadoEParseado() {
        String csv = """
                insumo_id;unidade_id;numero_lote;quantidade;valor_unitario;data_fabricacao;data_validade
                11111111-1111-1111-1111-111111111111;22222222-2222-2222-2222-222222222222;L-001;10;25.5;;2026-09-01
                """;
        var plan = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "carga.csv");

        assertThat(plan.linhas()).hasSize(1);
    }

    @Test
    void csv_uuidInvalido_lancaValidacaoComLinha() {
        String csv = """
                insumo_id,unidade_id,numero_lote,quantidade,valor_unitario,data_fabricacao,data_validade
                nao-eh-uuid,22222222-2222-2222-2222-222222222222,L-001,10,25.5,,2026-09-01
                """;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "x.csv"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Linha 2")
                .hasMessageContaining("insumo_id");
    }

    @Test
    void csv_quantidadeNegativa_lancaValidacao() {
        String csv = """
                insumo_id,unidade_id,numero_lote,quantidade,valor_unitario,data_fabricacao,data_validade
                11111111-1111-1111-1111-111111111111,22222222-2222-2222-2222-222222222222,L-001,-5,25.5,,2026-09-01
                """;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "x.csv"))
                .hasMessageContaining("positiva");
    }

    @Test
    void csv_apenasHeader_lancaValidacao() {
        String csv = "insumo_id,unidade_id,numero_lote,quantidade,valor_unitario,data_fabricacao,data_validade\n";
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "x.csv"))
                .hasMessageContaining("não contém linhas");
    }

    @Test
    void hashIguaisParaConteudoIgual() {
        String csv = """
                insumo_id,unidade_id,numero_lote,quantidade,valor_unitario,data_fabricacao,data_validade
                11111111-1111-1111-1111-111111111111,22222222-2222-2222-2222-222222222222,L-001,10,25.5,,2026-09-01
                """;
        var p1 = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "x.csv");
        var p2 = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "y.csv");
        assertThat(p1.hashSha256()).isEqualTo(p2.hashSha256());
    }
}
