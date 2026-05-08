package com.nonnas.catalog.application.conversao;

import com.nonnas.catalog.application.ports.ConversaoUnidadeRepository;
import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.application.ports.UnidadeMedidaRepository;
import com.nonnas.catalog.domain.ConversaoUnidade;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.catalog.domain.UnidadeMedidaId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

@Service
public class CriarConversaoUnidadeUseCase {

    private final ConversaoUnidadeRepository repository;
    private final UnidadeMedidaRepository unidadeRepo;
    private final InsumoRepository insumoRepo;
    private final Clock clock;

    public CriarConversaoUnidadeUseCase(ConversaoUnidadeRepository repository,
                                        UnidadeMedidaRepository unidadeRepo,
                                        InsumoRepository insumoRepo,
                                        Clock clock) {
        this.repository = repository;
        this.unidadeRepo = unidadeRepo;
        this.insumoRepo = insumoRepo;
        this.clock = clock;
    }

    @Transactional
    public ConversaoUnidade execute(UUID origemId, UUID destinoId, BigDecimal fator, UUID insumoId) {
        UnidadeMedidaId origem = UnidadeMedidaId.of(origemId);
        UnidadeMedidaId destino = UnidadeMedidaId.of(destinoId);
        if (unidadeRepo.findById(origem).isEmpty()) {
            throw new NotFoundException("Unidade de origem", origemId);
        }
        if (unidadeRepo.findById(destino).isEmpty()) {
            throw new NotFoundException("Unidade de destino", destinoId);
        }
        if (insumoId != null) {
            InsumoId iId = InsumoId.of(insumoId);
            if (insumoRepo.findById(iId).isEmpty()) {
                throw new NotFoundException("Insumo", insumoId);
            }
            return repository.save(ConversaoUnidade.porInsumo(origem, destino, fator, iId, clock.instant()));
        }
        return repository.save(ConversaoUnidade.global(origem, destino, fator, clock.instant()));
    }
}
