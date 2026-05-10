package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.catalog.domain.CategoriaInsumo;
import com.nonnas.catalog.domain.CategoriaInsumoId;
import com.nonnas.catalog.domain.Cnpj;
import com.nonnas.catalog.domain.ContatoFornecedor;
import com.nonnas.catalog.domain.ConversaoUnidade;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.catalog.domain.FornecedorId;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoFilial;
import com.nonnas.catalog.domain.InsumoFilialId;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.catalog.domain.UnidadeMedida;
import com.nonnas.catalog.domain.UnidadeMedidaId;

final class CatalogMappers {

    private CatalogMappers() {}

    // CategoriaInsumo
    static CategoriaInsumoEntity toEntity(CategoriaInsumo c) {
        CategoriaInsumoEntity e = new CategoriaInsumoEntity();
        e.setId(c.id().value());
        e.setCategoriaPaiId(c.categoriaPaiId().map(CategoriaInsumoId::value).orElse(null));
        e.setNome(c.nome());
        e.setAtiva(c.ativa());
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }

    static CategoriaInsumo toDomain(CategoriaInsumoEntity e) {
        return new CategoriaInsumo(
                CategoriaInsumoId.of(e.getId()),
                e.getCategoriaPaiId() != null ? CategoriaInsumoId.of(e.getCategoriaPaiId()) : null,
                e.getNome(),
                e.isAtiva(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // UnidadeMedida
    static UnidadeMedidaEntity toEntity(UnidadeMedida u) {
        UnidadeMedidaEntity e = new UnidadeMedidaEntity();
        e.setId(u.id().value());
        e.setCodigo(u.codigo());
        e.setNome(u.nome());
        e.setTipo(u.tipo());
        e.setAtiva(u.ativa());
        e.setCreatedAt(u.createdAt());
        e.setUpdatedAt(u.updatedAt());
        return e;
    }

    static UnidadeMedida toDomain(UnidadeMedidaEntity e) {
        return new UnidadeMedida(
                UnidadeMedidaId.of(e.getId()),
                e.getCodigo(),
                e.getNome(),
                e.getTipo(),
                e.isAtiva(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // ConversaoUnidade
    static ConversaoUnidadeEntity toEntity(ConversaoUnidade c) {
        ConversaoUnidadeEntity e = new ConversaoUnidadeEntity();
        e.setId(c.id());
        e.setInsumoId(c.insumoIdOpt().map(InsumoId::value).orElse(null));
        e.setUnidadeOrigemId(c.origemId().value());
        e.setUnidadeDestinoId(c.destinoId().value());
        e.setFator(c.fator());
        e.setCreatedAt(c.createdAt());
        return e;
    }

    static ConversaoUnidade toDomain(ConversaoUnidadeEntity e) {
        return new ConversaoUnidade(
                e.getId(),
                UnidadeMedidaId.of(e.getUnidadeOrigemId()),
                UnidadeMedidaId.of(e.getUnidadeDestinoId()),
                e.getFator(),
                e.getInsumoId() != null ? InsumoId.of(e.getInsumoId()) : null,
                e.getCreatedAt()
        );
    }

    // Fornecedor
    static FornecedorEntity toEntity(Fornecedor f) {
        FornecedorEntity e = new FornecedorEntity();
        e.setId(f.id().value());
        e.setRazaoSocial(f.razaoSocial());
        e.setCnpj(f.cnpj().value());
        e.setAtivo(f.ativo());
        e.setCreatedAt(f.createdAt());
        e.setUpdatedAt(f.updatedAt());
        e.getContatos().clear();
        for (ContatoFornecedor c : f.contatos()) {
            e.getContatos().add(toEntity(c, f.createdAt()));
        }
        return e;
    }

    static Fornecedor toDomain(FornecedorEntity e) {
        java.util.List<ContatoFornecedor> contatos = e.getContatos().stream()
                .map(CatalogMappers::toDomain).toList();
        return new Fornecedor(
                FornecedorId.of(e.getId()),
                e.getRazaoSocial(),
                Cnpj.of(e.getCnpj()),
                e.isAtivo(),
                contatos,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // ContatoFornecedor
    static ContatoFornecedorEntity toEntity(ContatoFornecedor c, java.time.Instant agora) {
        ContatoFornecedorEntity e = new ContatoFornecedorEntity();
        e.setId(c.id());
        e.setNome(c.nomeOpt().orElse(null));
        e.setEmail(c.emailOpt().orElse(null));
        e.setTelefone(c.telefoneOpt().orElse(null));
        e.setCreatedAt(agora);
        return e;
    }

    static ContatoFornecedor toDomain(ContatoFornecedorEntity e) {
        return new ContatoFornecedor(e.getId(), e.getNome(), e.getEmail(), e.getTelefone());
    }

    // Insumo
    static InsumoEntity toEntity(Insumo i) {
        InsumoEntity e = new InsumoEntity();
        e.setId(i.id().value());
        e.setCodigo(i.codigo());
        e.setNome(i.nome());
        e.setCategoriaId(i.categoriaId().value());
        e.setUnidadeBaseId(i.unidadeBaseId().value());
        e.setControlaLote(i.controlaLote());
        e.setControlaValidade(i.controlaValidade());
        e.setAtivo(i.ativo());
        e.setCreatedAt(i.createdAt());
        e.setUpdatedAt(i.updatedAt());
        return e;
    }

    static Insumo toDomain(InsumoEntity e) {
        return new Insumo(
                InsumoId.of(e.getId()),
                e.getCodigo(),
                e.getNome(),
                CategoriaInsumoId.of(e.getCategoriaId()),
                UnidadeMedidaId.of(e.getUnidadeBaseId()),
                e.isControlaLote(),
                e.isControlaValidade(),
                e.isAtivo(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // InsumoFilial
    static InsumoFilialEntity toEntity(InsumoFilial i) {
        InsumoFilialEntity e = new InsumoFilialEntity();
        e.setId(i.id().value());
        e.setInsumoId(i.insumoId().value());
        e.setFilialId(i.filialId());
        e.setEstoqueMinimo(i.estoqueMinimo());
        e.setEstoqueMaximo(i.estoqueMaximo().orElse(null));
        e.setPontoPedido(i.pontoPedido().orElse(null));
        e.setAtivo(i.ativo());
        e.setCreatedAt(i.createdAt());
        e.setUpdatedAt(i.updatedAt());
        return e;
    }

    static InsumoFilial toDomain(InsumoFilialEntity e) {
        return new InsumoFilial(
                InsumoFilialId.of(e.getId()),
                InsumoId.of(e.getInsumoId()),
                e.getFilialId(),
                e.getEstoqueMinimo(),
                e.getEstoqueMaximo(),
                e.getPontoPedido(),
                e.isAtivo(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
