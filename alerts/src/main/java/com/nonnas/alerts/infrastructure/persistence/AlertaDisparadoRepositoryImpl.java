package com.nonnas.alerts.infrastructure.persistence;

import com.nonnas.alerts.application.ports.AlertaDisparadoRepository;
import com.nonnas.alerts.domain.AlertaConfigId;
import com.nonnas.alerts.domain.AlertaDisparado;
import com.nonnas.alerts.domain.AlertaDisparadoId;
import com.nonnas.alerts.domain.StatusAlerta;
import com.nonnas.alerts.domain.TipoAlerta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class AlertaDisparadoRepositoryImpl implements AlertaDisparadoRepository {

    private final SpringDataAlertaDisparadoRepository jpa;

    @PersistenceContext
    private EntityManager em;

    AlertaDisparadoRepositoryImpl(SpringDataAlertaDisparadoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AlertaDisparado save(AlertaDisparado a) {
        return AlertsMappers.toDomain(jpa.save(AlertsMappers.toEntity(a)));
    }

    @Override
    public Optional<AlertaDisparado> findById(AlertaDisparadoId id) {
        return jpa.findById(id.value()).map(AlertsMappers::toDomain);
    }

    @Override
    public Optional<AlertaDisparado> findAtivoSemLote(AlertaConfigId configId, UUID insumoId, UUID filialId) {
        return jpa.findAtivoSemLote(configId.value(), insumoId, filialId).map(AlertsMappers::toDomain);
    }

    @Override
    public Optional<AlertaDisparado> findAtivoPorLote(AlertaConfigId configId, UUID loteId) {
        return jpa.findAtivoPorLote(configId.value(), loteId).map(AlertsMappers::toDomain);
    }

    @Override
    public List<AlertaDisparado> findAtivosPorEscopo(UUID insumoId, UUID filialId, TipoAlerta tipoOpt) {
        var resultados = jpa.findByStatusAndInsumoIdAndFilialId(StatusAlerta.ATIVO, insumoId, filialId);
        if (tipoOpt != null) {
            resultados = resultados.stream().filter(e -> e.getTipo() == tipoOpt).toList();
        }
        return resultados.stream().map(AlertsMappers::toDomain).toList();
    }

    @Override
    public List<AlertaDisparado> findAtivosPorLote(UUID loteId) {
        return jpa.findByStatusAndLoteId(StatusAlerta.ATIVO, loteId).stream()
                .map(AlertsMappers::toDomain).toList();
    }

    @Override
    public List<AlertaDisparado> listar(Filtros f, int page, int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<AlertaDisparadoEntity> q = cb.createQuery(AlertaDisparadoEntity.class);
        Root<AlertaDisparadoEntity> r = q.from(AlertaDisparadoEntity.class);
        List<Predicate> ps = new ArrayList<>();
        if (f.status() != null) ps.add(cb.equal(r.get("status"), f.status()));
        if (f.filialId() != null) ps.add(cb.equal(r.get("filialId"), f.filialId()));
        if (f.insumoId() != null) ps.add(cb.equal(r.get("insumoId"), f.insumoId()));
        if (f.tipo() != null) ps.add(cb.equal(r.get("tipo"), f.tipo()));
        if (f.dataDisparoDe() != null) ps.add(cb.greaterThanOrEqualTo(r.get("dataDisparo"), f.dataDisparoDe()));
        if (f.dataDisparoAte() != null) ps.add(cb.lessThanOrEqualTo(r.get("dataDisparo"), f.dataDisparoAte()));
        q.where(ps.toArray(new Predicate[0]));
        q.orderBy(cb.desc(r.get("dataDisparo")));
        return em.createQuery(q)
                .setFirstResult(page * size).setMaxResults(size)
                .getResultList().stream().map(AlertsMappers::toDomain).toList();
    }
}
