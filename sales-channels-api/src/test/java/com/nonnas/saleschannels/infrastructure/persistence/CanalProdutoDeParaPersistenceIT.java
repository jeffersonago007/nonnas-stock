package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.application.ports.CanalProdutoDeParaRepository;
import com.nonnas.saleschannels.domain.CanalProdutoDePara;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.testsupport.AbstractSalesChannelsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanalProdutoDeParaPersistenceIT extends AbstractSalesChannelsIntegrationTest {

    @Autowired private CanalProdutoDeParaRepository deparas;

    private static final Instant T0 = Instant.parse("2026-05-15T18:00:00Z");

    @Test
    void resolverPreferEspecificoDeFilialSobreGlobal() {
        UUID filial = UUID.randomUUID();
        UUID prodGlobal = UUID.randomUUID();
        UUID prodFilial = UUID.randomUUID();

        deparas.save(CanalProdutoDePara.novo(
                CanalTipo.IFOOD, "PIZZA-MARG", null, prodGlobal, "global", T0));
        deparas.save(CanalProdutoDePara.novo(
                CanalTipo.IFOOD, "PIZZA-MARG", filial, prodFilial, "específico filial", T0));

        Optional<CanalProdutoDePara> achadoEspecifico = deparas.resolver(
                CanalTipo.IFOOD, "PIZZA-MARG", filial);
        assertThat(achadoEspecifico).isPresent();
        assertThat(achadoEspecifico.get().produtoVendavelId()).isEqualTo(prodFilial);

        Optional<CanalProdutoDePara> achadoOutraFilial = deparas.resolver(
                CanalTipo.IFOOD, "PIZZA-MARG", UUID.randomUUID());
        assertThat(achadoOutraFilial).isPresent();
        assertThat(achadoOutraFilial.get().produtoVendavelId()).isEqualTo(prodGlobal);
    }

    @Test
    void resolverSemFilialSoMatchaGlobal() {
        UUID prodGlobal = UUID.randomUUID();
        deparas.save(CanalProdutoDePara.novo(
                CanalTipo.KEETA, "COMBO-1", null, prodGlobal, null, T0));

        Optional<CanalProdutoDePara> achado = deparas.resolver(
                CanalTipo.KEETA, "COMBO-1", null);
        assertThat(achado).isPresent();
        assertThat(achado.get().produtoVendavelId()).isEqualTo(prodGlobal);
    }

    @Test
    void resolverDevolveEmptyQuandoNaoCadastrado() {
        assertThat(deparas.resolver(CanalTipo.IFOOD, "SKU-INEXISTENTE", UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    void unicidadeGlobalImpedeMaisDeUmGlobalPorCanalECode() {
        deparas.save(CanalProdutoDePara.novo(
                CanalTipo.NOVENTANOVE_FOOD, "X", null, UUID.randomUUID(), null, T0));

        assertThatThrownBy(() -> deparas.save(CanalProdutoDePara.novo(
                CanalTipo.NOVENTANOVE_FOOD, "X", null, UUID.randomUUID(), null, T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unicidadePorFilialImpedeDuplicataParaMesmaFilial() {
        UUID filial = UUID.randomUUID();
        deparas.save(CanalProdutoDePara.novo(
                CanalTipo.IFOOD, "Y", filial, UUID.randomUUID(), null, T0));

        assertThatThrownBy(() -> deparas.save(CanalProdutoDePara.novo(
                CanalTipo.IFOOD, "Y", filial, UUID.randomUUID(), null, T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
