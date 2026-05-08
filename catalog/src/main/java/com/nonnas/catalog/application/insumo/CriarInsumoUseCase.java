package com.nonnas.catalog.application.insumo;

import com.nonnas.catalog.application.ports.CategoriaInsumoRepository;
import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.application.ports.UnidadeMedidaRepository;
import com.nonnas.catalog.domain.CategoriaInsumoId;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.UnidadeMedidaId;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class CriarInsumoUseCase {

    private final InsumoRepository insumoRepo;
    private final CategoriaInsumoRepository categoriaRepo;
    private final UnidadeMedidaRepository unidadeRepo;
    private final Clock clock;

    public CriarInsumoUseCase(InsumoRepository insumoRepo,
                              CategoriaInsumoRepository categoriaRepo,
                              UnidadeMedidaRepository unidadeRepo,
                              Clock clock) {
        this.insumoRepo = insumoRepo;
        this.categoriaRepo = categoriaRepo;
        this.unidadeRepo = unidadeRepo;
        this.clock = clock;
    }

    @Transactional
    public Insumo execute(String codigo, String nome, UUID categoriaId, UUID unidadeBaseId,
                          boolean controlaLote, boolean controlaValidade) {
        if (insumoRepo.existsByCodigo(codigo)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe insumo com código " + codigo);
        }
        CategoriaInsumoId catId = CategoriaInsumoId.of(categoriaId);
        if (categoriaRepo.findById(catId).isEmpty()) {
            throw new NotFoundException("Categoria de insumo", categoriaId);
        }
        UnidadeMedidaId unidId = UnidadeMedidaId.of(unidadeBaseId);
        if (unidadeRepo.findById(unidId).isEmpty()) {
            throw new NotFoundException("Unidade de medida", unidadeBaseId);
        }
        return insumoRepo.save(Insumo.novo(codigo, nome, catId, unidId, controlaLote, controlaValidade, clock.instant()));
    }
}
