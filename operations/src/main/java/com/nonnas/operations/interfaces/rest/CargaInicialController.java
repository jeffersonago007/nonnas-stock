package com.nonnas.operations.interfaces.rest;

import com.nonnas.operations.application.carga.PlanilhaCargaInicial;
import com.nonnas.operations.application.carga.ProcessarCargaInicialUseCase;
import com.nonnas.operations.infrastructure.importer.PlanilhaImporterService;
import com.nonnas.operations.interfaces.rest.dto.CargaInicialDto;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.http.HttpStatus;
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
    public CargaInicialDto.Response upload(
            @RequestParam("filialId") UUID filialId,
            @RequestParam("solicitadoPor") UUID solicitadoPor,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("Arquivo da planilha é obrigatório");
        }

        PlanilhaCargaInicial plan;
        try {
            plan = importer.parse(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new ValidationException("Falha ao ler arquivo: " + e.getMessage());
        }

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
}
