package com.nonnas.identity.application.filial;

import com.nonnas.identity.application.ports.FilialRepository;
import com.nonnas.identity.domain.Filial;
import com.nonnas.identity.domain.FilialId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AtualizarFilialUseCase {

    private final FilialRepository repository;
    private final Clock clock;

    public AtualizarFilialUseCase(FilialRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Filial execute(UUID id, String novoNome, String novoEndereco) {
        Filial filial = repository.findById(FilialId.of(id))
                .orElseThrow(() -> new NotFoundException("Filial", id));
        var agora = clock.instant();
        filial.renomear(novoNome, agora);
        filial.atualizarEndereco(novoEndereco, agora);
        return repository.save(filial);
    }
}
