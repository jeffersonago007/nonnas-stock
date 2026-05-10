package com.nonnas.identity.application.filial;

import com.nonnas.identity.application.ports.EmpresaRepository;
import com.nonnas.identity.application.ports.FilialRepository;
import com.nonnas.identity.domain.Cnpj;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.identity.domain.Filial;
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

    /**
     * Filiais de uma mesma rede podem compartilhar CNPJ (caso Nonnas Paola
     * — múltiplas lojas operando sob a mesma matriz fiscal). Por isso não
     * há checagem de unicidade de CNPJ aqui (UNIQUE removido em V013).
     * Validação de formato continua via {@link Cnpj#of(String)}.
     */
    @Transactional
    public Filial execute(UUID empresaId, String nome, String cnpj, String endereco) {
        EmpresaId empresaVo = EmpresaId.of(empresaId);
        if (empresaRepo.findById(empresaVo).isEmpty()) {
            throw new NotFoundException("Empresa", empresaId);
        }

        Cnpj cnpjVo = Cnpj.of(cnpj);
        Filial filial = Filial.nova(empresaVo, nome, cnpjVo, endereco, clock.instant());
        return filialRepo.save(filial);
    }
}
