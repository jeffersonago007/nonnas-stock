package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class InsumoRepositoryImpl implements InsumoRepository {

    private final SpringDataInsumoRepository jpa;

    InsumoRepositoryImpl(SpringDataInsumoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Insumo save(Insumo i) {
        return CatalogMappers.toDomain(jpa.save(CatalogMappers.toEntity(i)));
    }

    @Override
    public Optional<Insumo> findById(InsumoId id) {
        return jpa.findById(id.value()).map(CatalogMappers::toDomain);
    }

    @Override
    public Optional<Insumo> findByCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(CatalogMappers::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return jpa.existsByCodigo(codigo);
    }

    @Override
    public List<Insumo> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(CatalogMappers::toDomain).getContent();
    }

    @Override
    public List<Insumo> findFiltered(UUID categoriaId, Boolean ativo, String q, int page, int size) {
        // Hibernate não consegue inferir tipo de :q dentro de funções/CONCAT
        // no Postgres (cai pra bytea). Pattern montado em Java + LIKE :q
        // direto contorna isso e ainda faz lowercase consistente.
        String pattern = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        return jpa.findFiltered(categoriaId, ativo, pattern, PageRequest.of(page, size))
                .map(CatalogMappers::toDomain)
                .getContent();
    }
}
