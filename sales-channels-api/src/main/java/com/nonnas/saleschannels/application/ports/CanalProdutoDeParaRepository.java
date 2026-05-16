package com.nonnas.saleschannels.application.ports;

import com.nonnas.saleschannels.domain.CanalProdutoDePara;
import com.nonnas.saleschannels.domain.CanalProdutoDeParaId;
import com.nonnas.saleschannels.domain.CanalTipo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CanalProdutoDeParaRepository {

    CanalProdutoDePara save(CanalProdutoDePara depara);

    Optional<CanalProdutoDePara> findById(CanalProdutoDeParaId id);

    /**
     * Resolve external code com preferência por mapeamento específico de
     * filial. Lookup: 1) {@code (canal, code, filialId)}; 2) {@code (canal,
     * code, NULL)} (global). Retorna o primeiro match — ou empty se não há
     * de-para cadastrado.
     */
    Optional<CanalProdutoDePara> resolver(CanalTipo canal, String externalCode, UUID filialId);

    List<CanalProdutoDePara> listarPorCanal(CanalTipo canal);

    void delete(CanalProdutoDeParaId id);
}
