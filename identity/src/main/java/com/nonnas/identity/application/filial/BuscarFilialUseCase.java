package com.nonnas.identity.application.filial;

import com.nonnas.identity.application.ports.FilialRepository;
import com.nonnas.identity.domain.Filial;
import com.nonnas.identity.domain.FilialId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarFilialUseCase {

    private final FilialRepository repository;

    public BuscarFilialUseCase(FilialRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Filial execute(UUID id) {
        return repository.findById(FilialId.of(id))
                .orElseThrow(() -> new NotFoundException("Filial", id));
    }
}
