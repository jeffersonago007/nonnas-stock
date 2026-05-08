package com.nonnas.identity.application.ports;

import com.nonnas.identity.domain.Cnpj;
import com.nonnas.identity.domain.Empresa;
import com.nonnas.identity.domain.EmpresaId;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository {

    Empresa save(Empresa empresa);

    Optional<Empresa> findById(EmpresaId id);

    Optional<Empresa> findByCnpj(Cnpj cnpj);

    boolean existsByCnpj(Cnpj cnpj);

    List<Empresa> findAll(int page, int size);

    long count();
}
