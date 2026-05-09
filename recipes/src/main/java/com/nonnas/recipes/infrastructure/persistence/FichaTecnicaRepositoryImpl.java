package com.nonnas.recipes.infrastructure.persistence;

import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.FichaTecnicaId;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class FichaTecnicaRepositoryImpl implements FichaTecnicaRepository {

    private final SpringDataFichaTecnicaRepository jpa;

    FichaTecnicaRepositoryImpl(SpringDataFichaTecnicaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * {@code saveAndFlush} é deliberado: o use case {@code AtualizarFichaTecnica}
     * salva primeiro a ficha antiga (UPDATE ativa=false) e depois a nova (INSERT
     * ativa=true). Sem flush imediato, Hibernate poderia processar o INSERT
     * antes do UPDATE no flush final, violando o partial unique index
     * {@code uq_fichas_ativa_por_produto}.
     */
    @Override
    public FichaTecnica save(FichaTecnica f) {
        return RecipesMappers.toDomain(jpa.saveAndFlush(RecipesMappers.toEntity(f)));
    }

    @Override
    public Optional<FichaTecnica> findById(FichaTecnicaId id) {
        return jpa.findById(id.value()).map(RecipesMappers::toDomain);
    }

    @Override
    public Optional<FichaTecnica> findVigentePorProduto(ProdutoVendavelId produtoId) {
        return jpa.findByProdutoVendavelIdAndAtivaIsTrue(produtoId.value()).map(RecipesMappers::toDomain);
    }

    @Override
    public List<FichaTecnica> findHistoricoPorProduto(ProdutoVendavelId produtoId) {
        return jpa.findByProdutoVendavelIdOrderByVersaoDesc(produtoId.value())
                .stream().map(RecipesMappers::toDomain).toList();
    }
}
