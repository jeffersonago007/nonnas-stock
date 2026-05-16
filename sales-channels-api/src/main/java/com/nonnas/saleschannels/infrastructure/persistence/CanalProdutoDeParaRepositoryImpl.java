package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.application.ports.CanalProdutoDeParaRepository;
import com.nonnas.saleschannels.domain.CanalProdutoDePara;
import com.nonnas.saleschannels.domain.CanalProdutoDeParaId;
import com.nonnas.saleschannels.domain.CanalTipo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class CanalProdutoDeParaRepositoryImpl implements CanalProdutoDeParaRepository {

    private final SpringDataCanalProdutoDeParaRepository jpa;

    CanalProdutoDeParaRepositoryImpl(SpringDataCanalProdutoDeParaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CanalProdutoDePara save(CanalProdutoDePara depara) {
        CanalProdutoDeParaEntity e = new CanalProdutoDeParaEntity();
        e.setId(depara.id().value());
        e.setCanalTipo(depara.canalTipo());
        e.setExternalCode(depara.externalCode());
        e.setFilialId(depara.filialIdOpt().orElse(null));
        e.setProdutoVendavelId(depara.produtoVendavelId());
        e.setObservacao(depara.observacaoOpt().orElse(null));
        e.setCreatedAt(depara.createdAt());
        e.setUpdatedAt(depara.updatedAt());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<CanalProdutoDePara> findById(CanalProdutoDeParaId id) {
        return jpa.findById(id.value()).map(CanalProdutoDeParaRepositoryImpl::toDomain);
    }

    @Override
    public Optional<CanalProdutoDePara> resolver(CanalTipo canal, String externalCode, UUID filialId) {
        if (filialId != null) {
            Optional<CanalProdutoDeParaEntity> especifico = jpa
                    .findByCanalTipoAndExternalCodeAndFilialId(canal, externalCode, filialId);
            if (especifico.isPresent()) {
                return especifico.map(CanalProdutoDeParaRepositoryImpl::toDomain);
            }
        }
        return jpa.findGlobal(canal, externalCode).map(CanalProdutoDeParaRepositoryImpl::toDomain);
    }

    @Override
    public List<CanalProdutoDePara> listarPorCanal(CanalTipo canal) {
        return jpa.findByCanalTipoOrderByExternalCodeAsc(canal).stream()
                .map(CanalProdutoDeParaRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(CanalProdutoDeParaId id) {
        jpa.deleteById(id.value());
    }

    private static CanalProdutoDePara toDomain(CanalProdutoDeParaEntity e) {
        return new CanalProdutoDePara(
                CanalProdutoDeParaId.of(e.getId()),
                e.getCanalTipo(),
                e.getExternalCode(),
                e.getFilialId(),
                e.getProdutoVendavelId(),
                e.getObservacao(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
