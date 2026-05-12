package com.nonnas.operations.application.depara;

import com.nonnas.operations.application.ports.FornecedorInsumoDeParaRepository;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Apaga um mapeamento aprendido (cProd → insumo). A próxima nota daquele
 * fornecedor com esse código cai no fluxo normal (preview com COLISAO ou
 * LIVRE), permitindo ao operador re-decidir.
 */
@Service
public class ApagarDeParaUseCase {

    private final FornecedorInsumoDeParaRepository repo;

    public ApagarDeParaUseCase(FornecedorInsumoDeParaRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void execute(UUID fornecedorId, String codigoFornecedor) {
        if (codigoFornecedor == null || codigoFornecedor.isBlank()) {
            throw new ValidationException("Código do fornecedor é obrigatório");
        }
        repo.deleteByFornecedorAndCodigo(fornecedorId, codigoFornecedor);
    }
}
