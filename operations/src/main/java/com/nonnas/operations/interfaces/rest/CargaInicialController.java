package com.nonnas.operations.interfaces.rest;

import com.nonnas.operations.application.carga.PlanilhaCargaInicial;
import com.nonnas.operations.application.carga.ProcessarCargaInicialUseCase;
import com.nonnas.operations.infrastructure.importer.PlanilhaImporterService;
import com.nonnas.operations.interfaces.rest.dto.CargaInicialDto;
import com.nonnas.sharedkernel.ValidationException;
import com.nonnas.web.security.SecurityScope;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cargas-iniciais")
public class CargaInicialController {

    private final PlanilhaImporterService importer;
    private final ProcessarCargaInicialUseCase processar;

    public CargaInicialController(PlanilhaImporterService importer,
                                  ProcessarCargaInicialUseCase processar) {
        this.importer = importer;
        this.processar = processar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public CargaInicialDto.Response upload(
            @RequestParam("filialId") UUID filialId,
            @RequestParam("solicitadoPor") UUID solicitadoPor,
            @RequestParam("file") MultipartFile file) {
        SecurityScope.assertCanAccess(filialId);
        PlanilhaCargaInicial plan = parseOrThrow(file);

        var itens = plan.linhas().stream()
                .map(l -> new ProcessarCargaInicialUseCase.ItemEntrada(
                        l.insumoId(), l.unidadeId(), l.numeroLote(),
                        l.quantidade(), l.valorUnitario(),
                        l.dataFabricacao(), l.dataValidade()))
                .toList();
        var cmd = new ProcessarCargaInicialUseCase.Comando(
                filialId, plan.hashSha256(), plan.nomeArquivo(), solicitadoPor, itens);
        return CargaInicialDto.Response.from(processar.execute(cmd));
    }

    /**
     * Faz parse + validação da planilha sem persistir. Frontend usa o
     * resultado para mostrar preview ao usuário antes da confirmação
     * (master doc T13 — "preview antes de confirmar").
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public CargaInicialDto.PreviewResponse preview(@RequestParam("file") MultipartFile file) {
        return CargaInicialDto.PreviewResponse.from(parseOrThrow(file));
    }

    private PlanilhaCargaInicial parseOrThrow(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("Arquivo da planilha é obrigatório");
        }
        try {
            return importer.parse(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new ValidationException("Falha ao ler arquivo: " + e.getMessage());
        }
    }
}
