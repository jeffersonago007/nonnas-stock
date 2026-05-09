package com.nonnas.recipes.application.venda;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.recipes.application.ficha.AtualizarFichaTecnicaUseCase;
import com.nonnas.recipes.application.ficha.CriarFichaTecnicaUseCase;
import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.application.produto.CriarProdutoVendavelUseCase;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.testsupport.AbstractRecipesIntegrationTest;
import com.nonnas.sharedkernel.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VendaSimuladaIT extends AbstractRecipesIntegrationTest {

    @Autowired private CriarProdutoVendavelUseCase criarProduto;
    @Autowired private CriarFichaTecnicaUseCase criarFicha;
    @Autowired private AtualizarFichaTecnicaUseCase atualizarFicha;
    @Autowired private RegistrarVendaSimuladaUseCase venda;
    @Autowired private FichaTecnicaRepository fichaRepo;

    @Autowired private RegistrarEntradaManualUseCase entrada;
    @Autowired private SaldoLoteRepository saldoRepo;

    @Test
    void vendaSimulada_baixaInsumosViaFefo_atualizaSaldoCorretamente() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumoMussarela = UUID.randomUUID();
        UUID insumoMolho = UUID.randomUUID();
        UUID unidadeKg = UUID.randomUUID();

        // 2 lotes de mussarela (FEFO consome mais próximo primeiro)
        criarLote(filial, usuario, insumoMussarela, unidadeKg, "M-1", LocalDate.parse("2026-06-01"), 5);
        criarLote(filial, usuario, insumoMussarela, unidadeKg, "M-2", LocalDate.parse("2026-12-01"), 10);
        // 1 lote de molho
        criarLote(filial, usuario, insumoMolho, unidadeKg, "Mo-1", LocalDate.parse("2026-09-01"), 8);

        // Produto + ficha vigente: 200g mussarela + 100g molho por pizza
        ProdutoVendavel pizza = criarProduto.execute(
                new CriarProdutoVendavelUseCase.Comando("PIZ-MARG", "Pizza Margherita", "Pizza"));

        FichaTecnica vigente = criarFicha.execute(new CriarFichaTecnicaUseCase.Comando(
                pizza.id().value(),
                List.of(
                        new CriarFichaTecnicaUseCase.ItemEntrada(insumoMussarela, unidadeKg, new BigDecimal("0.2")),
                        new CriarFichaTecnicaUseCase.ItemEntrada(insumoMolho, unidadeKg, new BigDecimal("0.1"))
                )));

        // Venda de 10 pizzas → 2.0kg mussarela (5 do L1 vence primeiro? não, 0.2*10=2 ≤ 5 → tudo do L1)
        // 1.0kg molho → 1 do Mo-1
        Movimentacao mov = venda.execute(new RegistrarVendaSimuladaUseCase.Comando(
                pizza.id().value(), filial, usuario, new BigDecimal("10"), "venda teste"));

        assertThat(mov.tipo()).isEqualTo(TipoMovimentacao.SAIDA_VENDA);
        assertThat(mov.documentoOrigemTipoOpt()).contains("FICHA_TECNICA");
        assertThat(mov.documentoOrigemIdOpt()).contains(vigente.id().value());
        assertThat(mov.gerouNegativo()).isFalse();
        assertThat(mov.itens()).hasSize(2);  // 1 lote de mussarela + 1 lote de molho

        assertThat(saldoRepo.somarPorInsumoEFilial(insumoMussarela, filial))
                .isEqualByComparingTo("13");  // 15 - 2
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoMolho, filial))
                .isEqualByComparingTo("7");  // 8 - 1
    }

    @Test
    void venda_FEFO_consomeLoteVencidoPrimeiro() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        criarLote(filial, usuario, insumo, unidade, "L1-vence-junho", LocalDate.parse("2026-06-01"), 3);
        criarLote(filial, usuario, insumo, unidade, "L2-vence-dezembro", LocalDate.parse("2026-12-01"), 10);

        ProdutoVendavel produto = criarProduto.execute(
                new CriarProdutoVendavelUseCase.Comando("PROD-X", "Produto X", "Generico"));

        criarFicha.execute(new CriarFichaTecnicaUseCase.Comando(
                produto.id().value(),
                List.of(new CriarFichaTecnicaUseCase.ItemEntrada(insumo, unidade, new BigDecimal("1")))));

        // Vende 5 unidades → consome 3 do L1 (vence primeiro) + 2 do L2
        Movimentacao mov = venda.execute(new RegistrarVendaSimuladaUseCase.Comando(
                produto.id().value(), filial, usuario, new BigDecimal("5"), null));

        assertThat(mov.itens()).hasSize(2);
        assertThat(mov.itens().get(0).quantidadeBase()).isEqualByComparingTo("3");
        assertThat(mov.itens().get(1).quantidadeBase()).isEqualByComparingTo("2");
    }

    @Test
    void snapshot_vendaApontaParaVersaoVigenteNoMomento_edicaoPosteriorNaoAfetaHistorico() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        criarLote(filial, usuario, insumo, unidade, "S-1", LocalDate.parse("2026-12-01"), 100);

        ProdutoVendavel produto = criarProduto.execute(
                new CriarProdutoVendavelUseCase.Comando("PROD-S", "Produto Snapshot", "Generico"));

        FichaTecnica v1 = criarFicha.execute(new CriarFichaTecnicaUseCase.Comando(
                produto.id().value(),
                List.of(new CriarFichaTecnicaUseCase.ItemEntrada(insumo, unidade, new BigDecimal("1")))));

        // Venda registra a ficha v1
        Movimentacao mov = venda.execute(new RegistrarVendaSimuladaUseCase.Comando(
                produto.id().value(), filial, usuario, new BigDecimal("3"), null));
        assertThat(mov.documentoOrigemIdOpt()).contains(v1.id().value());

        // Atualiza ficha → cria v2 ativa, v1 vira histórico
        FichaTecnica v2 = atualizarFicha.execute(new AtualizarFichaTecnicaUseCase.Comando(
                produto.id().value(),
                List.of(new AtualizarFichaTecnicaUseCase.ItemEntrada(insumo, unidade, new BigDecimal("2")))));

        assertThat(v2.versao()).isEqualTo(2);
        assertThat(v2.ativa()).isTrue();
        assertThat(v2.id()).isNotEqualTo(v1.id());

        // Histórico tem ambas; v1 inativa, v2 ativa
        var historico = fichaRepo.findHistoricoPorProduto(ProdutoVendavelId.of(produto.id().value()));
        assertThat(historico).hasSize(2);
        assertThat(historico.get(0).versao()).isEqualTo(2);
        assertThat(historico.get(0).ativa()).isTrue();
        assertThat(historico.get(1).versao()).isEqualTo(1);
        assertThat(historico.get(1).ativa()).isFalse();
        assertThat(historico.get(1).vigenteAteOpt()).isPresent();

        // Movimentação anterior continua apontando para v1 (snapshot por referência)
        assertThat(mov.documentoOrigemIdOpt()).contains(v1.id().value());

        // Nova venda registra v2
        Movimentacao mov2 = venda.execute(new RegistrarVendaSimuladaUseCase.Comando(
                produto.id().value(), filial, usuario, new BigDecimal("1"), null));
        assertThat(mov2.documentoOrigemIdOpt()).contains(v2.id().value());
    }

    @Test
    void venda_semFichaVigente_retornaNotFound() {
        ProdutoVendavel sem = criarProduto.execute(
                new CriarProdutoVendavelUseCase.Comando("SEM-FICHA", "Produto Sem Ficha", "Generico"));

        assertThatThrownBy(() -> venda.execute(new RegistrarVendaSimuladaUseCase.Comando(
                sem.id().value(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1"), null)))
                .isInstanceOf(NotFoundException.class);
    }

    private void criarLote(UUID filial, UUID usuario, UUID insumo, UUID unidade,
                           String numeroLote, LocalDate validade, int qtd) {
        var cmd = new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, numeroLote,
                null, validade, new BigDecimal("10.00"),
                unidade, new BigDecimal(qtd), new BigDecimal(qtd),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null);
        entrada.execute(cmd);
    }
}
