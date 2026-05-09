package com.nonnas.identity.application.featureflags;

import com.nonnas.identity.application.ports.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock FeatureFlagRepository repository;

    @Test
    void chaveAusenteRetornaFalse() {
        when(repository.findByChave("inexistente")).thenReturn(Optional.empty());
        assertThat(new FeatureFlagService(repository).isAtiva("inexistente")).isFalse();
    }

    @Test
    void habilitadaFalsoSempreRetornaFalseMesmoComRolloutAlto() {
        when(repository.findByChave("k")).thenReturn(Optional.of(
                new FeatureFlag("k", "d", false, 100, Instant.now(), Instant.now())));
        assertThat(new FeatureFlagService(repository).isAtiva("k")).isFalse();
    }

    @Test
    void habilitadaTrueComRollout100SempreAtiva() {
        when(repository.findByChave("k")).thenReturn(Optional.of(
                new FeatureFlag("k", "d", true, 100, Instant.now(), Instant.now())));
        FeatureFlagService svc = new FeatureFlagService(repository);
        for (int i = 0; i < 50; i++) {
            assertThat(svc.isAtiva("k")).isTrue();
        }
    }

    @Test
    void rolloutZeroNuncaAtiva() {
        when(repository.findByChave("k")).thenReturn(Optional.of(
                new FeatureFlag("k", "d", true, 0, Instant.now(), Instant.now())));
        FeatureFlagService svc = new FeatureFlagService(repository);
        for (int i = 0; i < 50; i++) {
            assertThat(svc.isAtiva("k")).isFalse();
        }
    }

    @Test
    void rolloutIntermediarioGeraResultadoNaoDeterministicoMasDentroDeFaixa() {
        when(repository.findByChave("k")).thenReturn(Optional.of(
                new FeatureFlag("k", "d", true, 50, Instant.now(), Instant.now())));
        FeatureFlagService svc = new FeatureFlagService(repository);
        int trues = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            if (svc.isAtiva("k")) trues++;
        }
        // Esperado próximo de 50% — janela larga pra evitar flakiness.
        assertThat(trues).isBetween(350, 650);
    }
}
