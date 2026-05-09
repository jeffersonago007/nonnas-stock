package com.nonnas.operations.application.transferencia;

import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.domain.StatusTransferencia;
import com.nonnas.operations.domain.Transferencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListarTransferenciasUseCase {

    private final TransferenciaRepository repository;

    public ListarTransferenciasUseCase(TransferenciaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Transferencia> execute(UUID filialId, StatusTransferencia status, int page, int size) {
        return repository.findFiltered(filialId, status, page, size);
    }
}
