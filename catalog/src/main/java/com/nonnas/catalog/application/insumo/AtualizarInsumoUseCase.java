package com.nonnas.catalog.application.insumo;

import com.nonnas.catalog.application.ports.CategoriaInsumoRepository;
import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.domain.CategoriaInsumoId;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Atualiza campos editáveis de um insumo já cadastrado. Todos os campos
 * exceto {@code novoNome} são opcionais — o caller passa null para o que
 * não muda. {@code codigo} e {@code unidadeBaseId} permanecem imutáveis
 * (mexê-los quebraria histórico de movimentações e cálculos de saldo).
 */
@Service
public class AtualizarInsumoUseCase {

    private final InsumoRepository insumoRepo;
    private final CategoriaInsumoRepository categoriaRepo;
    private final Clock clock;

    public AtualizarInsumoUseCase(InsumoRepository insumoRepo,
                                  CategoriaInsumoRepository categoriaRepo,
                                  Clock clock) {
        this.insumoRepo = insumoRepo;
        this.categoriaRepo = categoriaRepo;
        this.clock = clock;
    }

    @Transactional
    public Insumo execute(UUID id, String novoNome,
                          UUID novaCategoriaId,
                          Boolean novoControlaLote,
                          Boolean novoControlaValidade) {
        Insumo insumo = insumoRepo.findById(InsumoId.of(id))
                .orElseThrow(() -> new NotFoundException("Insumo", id));
        Instant agora = clock.instant();

        if (novoNome != null) {
            insumo.renomear(novoNome, agora);
        }
        if (novaCategoriaId != null) {
            CategoriaInsumoId catId = CategoriaInsumoId.of(novaCategoriaId);
            if (categoriaRepo.findById(catId).isEmpty()) {
                throw new NotFoundException("Categoria de insumo", novaCategoriaId);
            }
            insumo.mudarCategoria(catId, agora);
        }
        if (novoControlaLote != null) {
            insumo.definirControlaLote(novoControlaLote, agora);
        }
        if (novoControlaValidade != null) {
            insumo.definirControlaValidade(novoControlaValidade, agora);
        }

        return insumoRepo.save(insumo);
    }
}
