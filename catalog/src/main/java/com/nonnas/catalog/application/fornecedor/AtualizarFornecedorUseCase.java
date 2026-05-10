package com.nonnas.catalog.application.fornecedor;

import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.domain.ContatoFornecedor;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.catalog.domain.FornecedorId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AtualizarFornecedorUseCase {

    private final FornecedorRepository repository;
    private final Clock clock;

    public AtualizarFornecedorUseCase(FornecedorRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Fornecedor execute(UUID id, String novaRazaoSocial) {
        return execute(id, novaRazaoSocial, null);
    }

    /**
     * Atualiza razão social e (opcionalmente) substitui a lista de contatos.
     * Quando {@code novosContatos} é {@code null}, mantém os contatos atuais
     * intactos; quando é uma lista (mesmo vazia), substitui completamente
     * (orphanRemoval limpa os antigos via JPA).
     */
    @Transactional
    public Fornecedor execute(UUID id, String novaRazaoSocial, List<ContatoFornecedor> novosContatos) {
        Fornecedor fornecedor = repository.findById(FornecedorId.of(id))
                .orElseThrow(() -> new NotFoundException("Fornecedor", id));
        Instant agora = clock.instant();
        if (novaRazaoSocial != null) {
            fornecedor.renomear(novaRazaoSocial, agora);
        }
        if (novosContatos != null) {
            fornecedor.definirContatos(novosContatos, agora);
        }
        return repository.save(fornecedor);
    }
}
