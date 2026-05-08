package com.nonnas.identity.application.ports;

import com.nonnas.identity.domain.Cnpj;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.identity.domain.Filial;
import com.nonnas.identity.domain.FilialId;

import java.util.List;
import java.util.Optional;

public interface FilialRepository {

    Filial save(Filial filial);

    Optional<Filial> findById(FilialId id);

    Optional<Filial> findByCnpj(Cnpj cnpj);

    boolean existsByCnpj(Cnpj cnpj);

    List<Filial> findByEmpresa(EmpresaId empresaId, int page, int size);

    List<Filial> findAll(int page, int size);

    long count();
}
