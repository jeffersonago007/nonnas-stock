package com.nonnas.recipes.infrastructure.persistence;

import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.FichaTecnicaId;
import com.nonnas.recipes.domain.ItemFichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.domain.TipoProdutoVendavel;

import java.util.List;

final class RecipesMappers {

    private RecipesMappers() {}

    // ProdutoVendavel
    static ProdutoVendavelEntity toEntity(ProdutoVendavel p) {
        ProdutoVendavelEntity e = new ProdutoVendavelEntity();
        e.setId(p.id().value());
        e.setCodigo(p.codigo());
        e.setNome(p.nome());
        e.setCategoria(p.categoria());
        e.setTipo(p.tipo().name());
        e.setInsumoRevendaId(p.insumoRevendaIdOpt().orElse(null));
        e.setAtivo(p.ativo());
        e.setCreatedAt(p.createdAt());
        e.setUpdatedAt(p.updatedAt());
        return e;
    }

    static ProdutoVendavel toDomain(ProdutoVendavelEntity e) {
        return new ProdutoVendavel(
                ProdutoVendavelId.of(e.getId()),
                e.getCodigo(),
                e.getNome(),
                e.getCategoria(),
                TipoProdutoVendavel.valueOf(e.getTipo()),
                e.getInsumoRevendaId(),
                e.isAtivo(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // ItemFichaTecnica
    static ItemFichaTecnicaEntity toEntity(ItemFichaTecnica i) {
        ItemFichaTecnicaEntity e = new ItemFichaTecnicaEntity();
        e.setId(i.id());
        e.setInsumoId(i.insumoId());
        e.setUnidadeId(i.unidadeId());
        e.setQuantidade(i.quantidade());
        return e;
    }

    static ItemFichaTecnica toDomain(ItemFichaTecnicaEntity e) {
        return new ItemFichaTecnica(e.getId(), e.getInsumoId(), e.getUnidadeId(), e.getQuantidade());
    }

    // FichaTecnica
    static FichaTecnicaEntity toEntity(FichaTecnica f) {
        FichaTecnicaEntity e = new FichaTecnicaEntity();
        e.setId(f.id().value());
        e.setProdutoVendavelId(f.produtoVendavelId().value());
        e.setVersao(f.versao());
        e.setVigenteDesde(f.vigenteDesde());
        e.setVigenteAte(f.vigenteAteOpt().orElse(null));
        e.setAtiva(f.ativa());
        e.setCreatedAt(f.createdAt());
        e.setUpdatedAt(f.updatedAt());
        e.getItens().clear();
        for (var item : f.itens()) {
            e.getItens().add(toEntity(item));
        }
        return e;
    }

    static FichaTecnica toDomain(FichaTecnicaEntity e) {
        List<ItemFichaTecnica> itens = e.getItens().stream()
                .map(RecipesMappers::toDomain)
                .toList();
        return new FichaTecnica(
                FichaTecnicaId.of(e.getId()),
                ProdutoVendavelId.of(e.getProdutoVendavelId()),
                e.getVersao(),
                e.getVigenteDesde(),
                e.getVigenteAte(),
                e.isAtiva(),
                itens,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
