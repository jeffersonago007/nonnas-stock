package com.nonnas.catalog.application.ports;

import com.nonnas.catalog.domain.ConversaoUnidade;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.catalog.domain.UnidadeMedidaId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversaoUnidadeRepository {

    ConversaoUnidade save(ConversaoUnidade c);

    Optional<ConversaoUnidade> findById(UUID id);

    /**
     * @return conversão específica para esse insumo no sentido origem→destino, se houver.
     */
    Optional<ConversaoUnidade> findByInsumoEOrigemDestino(InsumoId insumoId,
                                                          UnidadeMedidaId origem,
                                                          UnidadeMedidaId destino);

    /**
     * @return conversão global (insumo_id IS NULL) no sentido origem→destino, se houver.
     */
    Optional<ConversaoUnidade> findGlobalPorOrigemDestino(UnidadeMedidaId origem,
                                                          UnidadeMedidaId destino);

    List<ConversaoUnidade> findAll(int page, int size);
}
