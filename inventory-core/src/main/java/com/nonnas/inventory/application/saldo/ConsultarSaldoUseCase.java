package com.nonnas.inventory.application.saldo;

import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ConsultarSaldoUseCase {

    private final SaldoLoteRepository repo;

    public ConsultarSaldoUseCase(SaldoLoteRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoPorInsumoEFilial(UUID insumoId, UUID filialId) {
        return repo.somarPorInsumoEFilial(insumoId, filialId);
    }
}
