package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.application.ports.CredencialCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.CredencialCanalId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class CredencialCanalRepositoryImpl implements CredencialCanalRepository {

    private final SpringDataCredencialCanalRepository jpa;

    CredencialCanalRepositoryImpl(SpringDataCredencialCanalRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CredencialCanal save(CredencialCanal credencial) {
        return SalesChannelsMappers.toDomain(jpa.save(SalesChannelsMappers.toEntity(credencial)));
    }

    @Override
    public Optional<CredencialCanal> findById(CredencialCanalId id) {
        return jpa.findById(id.value()).map(SalesChannelsMappers::toDomain);
    }

    @Override
    public Optional<CredencialCanal> findAtivaByCanalEFilial(CanalTipo canalTipo, UUID filialId) {
        return jpa.findByCanalTipoAndFilialIdAndAtivaIsTrue(canalTipo, filialId)
                .map(SalesChannelsMappers::toDomain);
    }

    @Override
    public Optional<CredencialCanal> findAtivaByMerchantExterno(CanalTipo canalTipo, String merchantExternoId) {
        return jpa.findByCanalTipoAndMerchantExternoIdAndAtivaIsTrue(canalTipo, merchantExternoId)
                .map(SalesChannelsMappers::toDomain);
    }

    @Override
    public List<CredencialCanal> listarPorCanal(CanalTipo canalTipo) {
        return jpa.findByCanalTipo(canalTipo).stream()
                .map(SalesChannelsMappers::toDomain).toList();
    }

    @Override
    public List<CredencialCanal> listarTodas() {
        return jpa.findAll().stream().map(SalesChannelsMappers::toDomain).toList();
    }
}
