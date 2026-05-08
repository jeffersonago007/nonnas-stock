package com.nonnas.identity.application.filial;

import com.nonnas.identity.application.ports.EmpresaRepository;
import com.nonnas.identity.application.ports.FilialRepository;
import com.nonnas.identity.domain.Cnpj;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.identity.domain.Filial;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class CriarFilialUseCase {

    private final FilialRepository filialRepo;
    private final EmpresaRepository empresaRepo;
    private final Clock clock;

    public CriarFilialUseCase(FilialRepository filialRepo, EmpresaRepository empresaRepo, Clock clock) {
        this.filialRepo = filialRepo;
        this.empresaRepo = empresaRepo;
        this.clock = clock;
    }

    @Transactional
    public Filial execute(UUID empresaId, String nome, String cnpj, String endereco) {
        EmpresaId empresaVo = EmpresaId.of(empresaId);
        if (empresaRepo.findById(empresaVo).isEmpty()) {
            throw new NotFoundException("Empresa", empresaId);
        }

        Cnpj cnpjVo = Cnpj.of(cnpj);
        if (filialRepo.existsByCnpj(cnpjVo)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe filial com CNPJ " + cnpjVo.formatted());
        }

        Filial filial = Filial.nova(empresaVo, nome, cnpjVo, endereco, clock.instant());
        return filialRepo.save(filial);
    }
}
