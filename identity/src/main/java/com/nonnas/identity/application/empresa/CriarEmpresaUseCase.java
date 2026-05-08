package com.nonnas.identity.application.empresa;

import com.nonnas.identity.application.ports.EmpresaRepository;
import com.nonnas.identity.domain.Cnpj;
import com.nonnas.identity.domain.Empresa;
import com.nonnas.identity.domain.RazaoSocial;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CriarEmpresaUseCase {

    private final EmpresaRepository repository;
    private final Clock clock;

    public CriarEmpresaUseCase(EmpresaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Empresa execute(String razaoSocial, String cnpj) {
        RazaoSocial razao = RazaoSocial.of(razaoSocial);
        Cnpj cnpjVo = Cnpj.of(cnpj);

        if (repository.existsByCnpj(cnpjVo)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe empresa cadastrada com CNPJ " + cnpjVo.formatted());
        }

        Empresa empresa = Empresa.nova(razao, cnpjVo, clock.instant());
        return repository.save(empresa);
    }
}
