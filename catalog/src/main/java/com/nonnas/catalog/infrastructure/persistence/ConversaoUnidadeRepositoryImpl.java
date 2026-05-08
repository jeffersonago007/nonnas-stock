package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.catalog.application.ports.ConversaoUnidadeRepository;
import com.nonnas.catalog.domain.ConversaoUnidade;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.catalog.domain.UnidadeMedidaId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ConversaoUnidadeRepositoryImpl implements ConversaoUnidadeRepository {

    private final SpringDataConversaoUnidadeRepository jpa;

    ConversaoUnidadeRepositoryImpl(SpringDataConversaoUnidadeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ConversaoUnidade save(ConversaoUnidade c) {
        return CatalogMappers.toDomain(jpa.save(CatalogMappers.toEntity(c)));
    }

    @Override
    public Optional<ConversaoUnidade> findById(UUID id) {
        return jpa.findById(id).map(CatalogMappers::toDomain);
    }

    @Override
    public Optional<ConversaoUnidade> findByInsumoEOrigemDestino(InsumoId insumoId,
                                                                 UnidadeMedidaId origem,
                                                                 UnidadeMedidaId destino) {
        return jpa.findByInsumoEOrigemDestino(insumoId.value(), origem.value(), destino.value())
                .map(CatalogMappers::toDomain);
    }

    @Override
    public Optional<ConversaoUnidade> findGlobalPorOrigemDestino(UnidadeMedidaId origem,
                                                                  UnidadeMedidaId destino) {
        return jpa.findGlobalPorOrigemDestino(origem.value(), destino.value())
                .map(CatalogMappers::toDomain);
    }

    @Override
    public List<ConversaoUnidade> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(CatalogMappers::toDomain).getContent();
    }
}
