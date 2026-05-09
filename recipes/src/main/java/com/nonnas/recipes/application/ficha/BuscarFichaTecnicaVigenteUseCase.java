package com.nonnas.recipes.application.ficha;

import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarFichaTecnicaVigenteUseCase {

    private final FichaTecnicaRepository fichaRepo;

    public BuscarFichaTecnicaVigenteUseCase(FichaTecnicaRepository fichaRepo) {
        this.fichaRepo = fichaRepo;
    }

    @Transactional(readOnly = true)
    public FichaTecnica execute(UUID produtoVendavelId) {
        return fichaRepo.findVigentePorProduto(ProdutoVendavelId.of(produtoVendavelId))
                .orElseThrow(() -> new NotFoundException(
                        "Ficha técnica vigente para o produto " + produtoVendavelId + " não encontrada"));
    }
}
