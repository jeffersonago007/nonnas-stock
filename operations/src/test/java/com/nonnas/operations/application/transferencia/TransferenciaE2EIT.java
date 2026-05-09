package com.nonnas.operations.application.transferencia;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.operations.application.ports.AjusteEstoqueRepository;
import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.domain.StatusAjuste;
import com.nonnas.operations.domain.StatusTransferencia;
import com.nonnas.operations.domain.Transferencia;
import com.nonnas.operations.testsupport.AbstractOperationsIntegrationTest;
import com.nonnas.sharedkernel.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferenciaE2EIT extends AbstractOperationsIntegrationTest {

    @Autowired private SolicitarTransferenciaUseCase solicitar;
    @Autowired private AprovarTransferenciaUseCase aprovar;
    @Autowired private RegistrarEnvioTransferenciaUseCase enviar;
    @Autowired private RegistrarRecebimentoTransferenciaUseCase receber;
    @Autowired private CancelarTransferenciaUseCase cancelar;
    @Autowired private AjusteEstoqueRepository ajusteRepo;
    @Autowired private TransferenciaRepository transfRepo;
    @Autowired private RegistrarEntradaManualUseCase entradaInv;
    @Autowired private SaldoLoteRepository saldoRepo;

    @Test
    void e2eTransferencia_saldoOrigemDesce_saldoDestinoSobe() {
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();
        UUID solicitante = UUID.randomUUID();
        UUID gerente = UUID.randomUUID();
        UUID operador = UUID.randomUUID();
        UUID conferente = UUID.randomUUID();
        UUID insumoMussarela = UUID.randomUUID();
        UUID insumoAzeite = UUID.randomUUID();
        UUID kg = UUID.randomUUID();

        // Origem tem 50kg de mussarela e 20L de azeite
        criarLoteOrigem(origem, solicitante, insumoMussarela, kg, "MUSS-1", "2026-09-01", 50);
        criarLoteOrigem(origem, solicitante, insumoAzeite, kg, "AZ-1", "2027-01-01", 20);

        // 1. Solicita
        var solicitada = solicitar.execute(new SolicitarTransferenciaUseCase.Comando(
                origem, destino, solicitante, "transferência semanal",
                List.of(
                        new SolicitarTransferenciaUseCase.ItemEntrada(insumoMussarela, kg, new BigDecimal("10")),
                        new SolicitarTransferenciaUseCase.ItemEntrada(insumoAzeite, kg, new BigDecimal("3"))
                )));
        assertThat(solicitada.status()).isEqualTo(StatusTransferencia.SOLICITADA);

        // 2. Aprova
        var aprovada = aprovar.execute(solicitada.id().value(), gerente);
        assertThat(aprovada.status()).isEqualTo(StatusTransferencia.APROVADA);

        // 3. Envia → SAIDA_TRANSFERENCIA na origem
        var enviada = enviar.execute(solicitada.id().value(), operador);
        assertThat(enviada.status()).isEqualTo(StatusTransferencia.EM_TRANSITO);
        assertThat(enviada.movSaidaIdOpt()).isPresent();

        // Saldo origem após envio: mussarela 50→40, azeite 20→17
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoMussarela, origem)).isEqualByComparingTo("40");
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoAzeite, origem)).isEqualByComparingTo("17");
        // Destino ainda zerado
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoMussarela, destino)).isEqualByComparingTo("0");

        // Endpoint /em-transito agrega quantidades das transferências EM_TRANSITO
        var emTransito = transfRepo.agregadoEmTransito(null);
        var mussEmTransito = emTransito.stream()
                .filter(r -> r.insumoId().equals(insumoMussarela)).findFirst().orElseThrow();
        assertThat(mussEmTransito.quantidadeEmTransito()).isEqualByComparingTo("10");
        // Filtro por filial destino traz apenas as endereçadas a ela
        assertThat(transfRepo.agregadoEmTransito(destino)).hasSize(2);
        assertThat(transfRepo.agregadoEmTransito(UUID.randomUUID())).isEmpty();

        // 4. Recebe — quantidades batendo (sem divergência)
        UUID itemMussId = enviada.itens().stream()
                .filter(i -> i.insumoId().equals(insumoMussarela)).findFirst().orElseThrow().id();
        UUID itemAzId = enviada.itens().stream()
                .filter(i -> i.insumoId().equals(insumoAzeite)).findFirst().orElseThrow().id();

        var recebida = receber.execute(new RegistrarRecebimentoTransferenciaUseCase.Comando(
                solicitada.id().value(), conferente,
                Map.of(itemMussId, new BigDecimal("10"), itemAzId, new BigDecimal("3"))));

        assertThat(recebida.status()).isEqualTo(StatusTransferencia.RECEBIDA);
        assertThat(recebida.movEntradaIdOpt()).isPresent();

        // Saldo destino após recebimento
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoMussarela, destino)).isEqualByComparingTo("10");
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoAzeite, destino)).isEqualByComparingTo("3");
        // Soma global: mussarela 40+10=50, azeite 17+3=20 — preservada
    }

    @Test
    void recebimentoComDivergencia_geraAjusteEstoqueParaAuditoria() {
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();
        UUID solicitante = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID kg = UUID.randomUUID();

        criarLoteOrigem(origem, solicitante, insumo, kg, "L-1", "2026-12-01", 30);

        var t = solicitar.execute(new SolicitarTransferenciaUseCase.Comando(
                origem, destino, solicitante, null,
                List.of(new SolicitarTransferenciaUseCase.ItemEntrada(insumo, kg, new BigDecimal("10")))));
        aprovar.execute(t.id().value(), solicitante);
        enviar.execute(t.id().value(), solicitante);

        UUID itemId = t.itens().get(0).id();
        // Recebe só 8 — divergência de -2
        receber.execute(new RegistrarRecebimentoTransferenciaUseCase.Comando(
                t.id().value(), solicitante, Map.of(itemId, new BigDecimal("8"))));

        // Saldo destino = 8 (qtd recebida); origem = 30 - 10 = 20.
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, destino)).isEqualByComparingTo("8");
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, origem)).isEqualByComparingTo("20");

        // Ajuste de auditoria criado, status=APROVADO (diff=-2 está abaixo do threshold 50)
        var ajustes = ajusteRepo.findByFilialEStatus(destino, StatusAjuste.APROVADO, 0, 100);
        assertThat(ajustes).hasSize(1);
        assertThat(ajustes.get(0).quantidadeDiff()).isEqualByComparingTo("-2");
        assertThat(ajustes.get(0).origemTransferenciaIdOpt()).contains(t.id().value());
        // Sem mov_id porque é só documentação (perda já implícita)
        assertThat(ajustes.get(0).movIdOpt()).isEmpty();
    }

    @Test
    void cancelar_transferencia_aposEnvio_falha() {
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID kg = UUID.randomUUID();

        criarLoteOrigem(origem, usuario, insumo, kg, "L-X", "2026-12-01", 5);
        var t = solicitar.execute(new SolicitarTransferenciaUseCase.Comando(
                origem, destino, usuario, null,
                List.of(new SolicitarTransferenciaUseCase.ItemEntrada(insumo, kg, new BigDecimal("2")))));
        aprovar.execute(t.id().value(), usuario);
        enviar.execute(t.id().value(), usuario);

        assertThatThrownBy(() -> cancelar.execute(t.id().value(), "tarde demais"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void enviar_semSaldoSuficiente_geraNegativo() {
        // Saída via FEFO permite saldo negativo (regra master doc 5.2).
        // Confirmamos que o envio prossegue mesmo sem estoque disponível,
        // marcando a movimentação como gerouNegativo.
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID kg = UUID.randomUUID();

        criarLoteOrigem(origem, usuario, insumo, kg, "L-3", "2026-12-01", 2);
        var t = solicitar.execute(new SolicitarTransferenciaUseCase.Comando(
                origem, destino, usuario, null,
                List.of(new SolicitarTransferenciaUseCase.ItemEntrada(insumo, kg, new BigDecimal("10")))));
        aprovar.execute(t.id().value(), usuario);
        Transferencia enviada = enviar.execute(t.id().value(), usuario);

        assertThat(enviada.status()).isEqualTo(StatusTransferencia.EM_TRANSITO);
        // Saldo origem = 2 - 10 = -8
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, origem)).isEqualByComparingTo("-8");
    }

    private void criarLoteOrigem(UUID filial, UUID usuario, UUID insumo, UUID unidade,
                                  String numeroLote, String validade, int qtd) {
        var cmd = new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, numeroLote,
                null, LocalDate.parse(validade), new BigDecimal("10"),
                unidade, new BigDecimal(qtd), new BigDecimal(qtd),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null);
        entradaInv.execute(cmd);
    }
}
