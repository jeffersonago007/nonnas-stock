package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.application.ports.NotaFiscalRepository;
import com.nonnas.operations.domain.NotaFiscal;
import com.nonnas.operations.domain.NotaFiscalId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class NotaFiscalRepositoryImpl implements NotaFiscalRepository {

    private final SpringDataNotaFiscalRepository jpa;

    @PersistenceContext
    private EntityManager em;

    NotaFiscalRepositoryImpl(SpringDataNotaFiscalRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public NotaFiscal save(NotaFiscal nota) {
        return OperationsMappers.toDomain(jpa.save(OperationsMappers.toEntity(nota)));
    }

    @Override
    public Optional<NotaFiscal> findById(NotaFiscalId id) {
        return jpa.findById(id.value()).map(OperationsMappers::toDomain);
    }

    @Override
    public Optional<NotaFiscal> findByChaveNfe(String chaveNfe) {
        return jpa.findByChaveNfe(chaveNfe).map(OperationsMappers::toDomain);
    }

    @Override
    public boolean existsByChaveNfe(String chaveNfe) {
        return jpa.existsByChaveNfe(chaveNfe);
    }

    /**
     * Usa Criteria dinâmico em vez de JPQL com {@code :param IS NULL} porque o
     * Postgres rejeita binds null sem tipo explícito (SQLSTATE 42P18, "não foi
     * possível determinar o tipo de dados do parâmetro"). Aqui só montamos
     * predicados para os filtros realmente preenchidos.
     */
    @Override
    public List<NotaFiscal> findFiltered(UUID filialId, Instant emissaoDe,
                                         Instant emissaoAte, int page, int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<NotaFiscalEntity> q = cb.createQuery(NotaFiscalEntity.class);
        Root<NotaFiscalEntity> root = q.from(NotaFiscalEntity.class);

        List<Predicate> preds = new ArrayList<>();
        if (filialId != null) {
            preds.add(cb.equal(root.get("filialId"), filialId));
        }
        if (emissaoDe != null) {
            preds.add(cb.greaterThanOrEqualTo(root.<Instant>get("dataEmissao"), emissaoDe));
        }
        if (emissaoAte != null) {
            preds.add(cb.lessThanOrEqualTo(root.<Instant>get("dataEmissao"), emissaoAte));
        }
        q.select(root)
         .where(preds.toArray(Predicate[]::new))
         .orderBy(cb.desc(root.get("dataEmissao")));

        return em.createQuery(q)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(OperationsMappers::toDomain)
                .toList();
    }
}
