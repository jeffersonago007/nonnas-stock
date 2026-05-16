package com.nonnas.saleschannels.infrastructure.schedule;

import com.nonnas.saleschannels.application.ProcessarPedidoCanalUseCase;
import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.domain.TipoEventoCanal;
import com.nonnas.saleschannels.infrastructure.config.SalesChannelsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessarPedidosSchedulerTest {

    private static final Instant T0 = Instant.parse("2026-05-15T18:00:00Z");

    private EventoCanalRepository eventos;
    private ProcessarPedidoCanalUseCase useCase;
    private ProcessarPedidosScheduler scheduler;

    @BeforeEach
    void setUp() {
        eventos = mock(EventoCanalRepository.class);
        useCase = mock(ProcessarPedidoCanalUseCase.class);
    }

    @Test
    void disabledNaoChamaUseCase() {
        scheduler = new ProcessarPedidosScheduler(eventos, useCase, propsEnabled(false));
        scheduler.processar();
        verifyNoInteractions(eventos);
        verifyNoInteractions(useCase);
    }

    @Test
    void enabledProcessaTodosOsEventosPendentes() {
        EventoCanal e1 = novoEvento("evt-1");
        EventoCanal e2 = novoEvento("evt-2");
        when(eventos.listarNaoProcessados(50)).thenReturn(List.of(e1, e2));
        scheduler = new ProcessarPedidosScheduler(eventos, useCase, propsEnabled(true));

        scheduler.processar();

        verify(useCase).processarEventoNaoProcessado(eq(e1.id().value()), eq(ProcessarPedidosScheduler.USUARIO_SISTEMA_SCHEDULER));
        verify(useCase).processarEventoNaoProcessado(eq(e2.id().value()), eq(ProcessarPedidosScheduler.USUARIO_SISTEMA_SCHEDULER));
        verify(useCase, times(2)).processarEventoNaoProcessado(any(), any());
    }

    @Test
    void falhaEmEventoIndividualNaoInterrompeBatch() {
        EventoCanal e1 = novoEvento("evt-1");
        EventoCanal e2 = novoEvento("evt-2");
        EventoCanal e3 = novoEvento("evt-3");
        when(eventos.listarNaoProcessados(50)).thenReturn(List.of(e1, e2, e3));
        doThrow(new RuntimeException("falha sintética"))
                .when(useCase).processarEventoNaoProcessado(eq(e2.id().value()), any());
        scheduler = new ProcessarPedidosScheduler(eventos, useCase, propsEnabled(true));

        int sucesso = scheduler.processarBatch();

        // Apesar da falha em e2, e3 também é processado.
        verify(useCase, times(3)).processarEventoNaoProcessado(any(), any());
        assertThat(sucesso).isEqualTo(2);
    }

    @Test
    void batchVazioNaoChamaUseCase() {
        when(eventos.listarNaoProcessados(50)).thenReturn(List.of());
        scheduler = new ProcessarPedidosScheduler(eventos, useCase, propsEnabled(true));

        scheduler.processar();

        verify(useCase, never()).processarEventoNaoProcessado(any(), any());
    }

    private SalesChannelsProperties propsEnabled(boolean enabled) {
        return new SalesChannelsProperties(
                new SalesChannelsProperties.Polling(false, Duration.ofSeconds(30), 50),
                new SalesChannelsProperties.Processar(enabled, Duration.ofSeconds(60), 50),
                new SalesChannelsProperties.Http(Duration.ofSeconds(5), Duration.ofSeconds(15)));
    }

    private EventoCanal novoEvento(String eventId) {
        return EventoCanal.recebido(
                CanalTipo.OPEN_DELIVERY_GENERICO,
                eventId,
                TipoEventoCanal.PEDIDO_CRIADO,
                UUID.randomUUID().toString(),
                "{}",
                T0);
    }
}
