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
public class DesativarFilialUseCase {

    private final FilialRepository repository;
    private final Clock clock;

    public DesativarFilialUseCase(FilialRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Filial execute(UUID id) {
        Filial filial = repository.findById(FilialId.of(id))
                .orElseThrow(() -> new NotFoundException("Filial", id));
        filial.desativar(clock.instant());
        return repository.save(filial);
    }
}
