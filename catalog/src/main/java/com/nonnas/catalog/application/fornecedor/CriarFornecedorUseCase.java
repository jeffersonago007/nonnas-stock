package com.nonnas.catalog.application.fornecedor;

import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.domain.Cnpj;
import com.nonnas.catalog.domain.ContatoFornecedor;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class CriarFornecedorUseCase {

    private final FornecedorRepository repository;
    private final Clock clock;

    public CriarFornecedorUseCase(FornecedorRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Fornecedor execute(String razaoSocial, String cnpj) {
        return execute(razaoSocial, cnpj, List.of());
    }

    @Transactional
    public Fornecedor execute(String razaoSocial, String cnpj, List<ContatoFornecedor> contatos) {
        Cnpj cnpjVo = Cnpj.of(cnpj);
        if (repository.existsByCnpj(cnpjVo)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe fornecedor com CNPJ " + cnpjVo.formatted());
        }
        return repository.save(Fornecedor.novo(razaoSocial, cnpjVo, contatos, clock.instant()));
    }
}
