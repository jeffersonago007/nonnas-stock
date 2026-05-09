package com.nonnas.operations.application.transferencia;

import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.domain.Transferencia;
import com.nonnas.operations.domain.TransferenciaId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AprovarTransferenciaUseCase {

    private final TransferenciaRepository repo;
    private final Clock clock;

    public AprovarTransferenciaUseCase(TransferenciaRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional
    public Transferencia execute(UUID transferenciaId, UUID aprovadoPor) {
        Transferencia t = repo.findById(TransferenciaId.of(transferenciaId))
                .orElseThrow(() -> new NotFoundException("Transferência", transferenciaId));
        t.aprovar(aprovadoPor, clock.instant());
        return repo.save(t);
    }
}
