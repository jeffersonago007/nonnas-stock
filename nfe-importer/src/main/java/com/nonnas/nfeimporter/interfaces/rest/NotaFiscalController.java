package com.nonnas.nfeimporter.interfaces.rest;

import com.nonnas.nfeimporter.application.PreviewNotaFiscalUseCase;
import com.nonnas.nfeimporter.application.ProcessarNotaFiscalUseCase;
import com.nonnas.nfeimporter.interfaces.rest.dto.NotaFiscalDto;
import com.nonnas.operations.application.notafiscal.BuscarNotaFiscalUseCase;
import com.nonnas.operations.application.notafiscal.ListarNotasFiscaisUseCase;
import com.nonnas.operations.application.ports.NotaFiscalRepository;
import com.nonnas.sharedkernel.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notas-fiscais")
public class NotaFiscalController {

    private final PreviewNotaFiscalUseCase preview;
    private final ProcessarNotaFiscalUseCase processar;
    private final BuscarNotaFiscalUseCase buscar;
    private final ListarNotasFiscaisUseCase listar;

    public NotaFiscalController(PreviewNotaFiscalUseCase preview,
                                ProcessarNotaFiscalUseCase processar,
                                BuscarNotaFiscalUseCase buscar,
                                ListarNotasFiscaisUseCase listar) {
        this.preview = preview;
        this.processar = processar;
        this.buscar = buscar;
        this.listar = listar;
    }

    /**
     * Faz parse do XML e devolve a representação intermediária para o
     * frontend pré-preencher o formulário de lançamento. Não persiste nada.
     */
    @PostMapping("/preview-xml")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public NotaFiscalDto.PreviewResponse previewXml(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Arquivo XML da NF-e é obrigatório");
        }
        try {
            return NotaFiscalDto.PreviewResponse.from(preview.execute(file.getInputStream()));
        } catch (IOException ex) {
            throw new ValidationException("Falha ao ler o arquivo: " + ex.getMessage());
        }
    }

    @PostMapping("/lancar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public NotaFiscalDto.Response lancar(@Valid @RequestBody NotaFiscalDto.LancarRequest req) {
        var fornecedorEntrada = (req.fornecedor().id() != null)
                ? ProcessarNotaFiscalUseCase.FornecedorEntrada.existente(req.fornecedor().id())
                : ProcessarNotaFiscalUseCase.FornecedorEntrada.novo(
                        req.fornecedor().cnpj(), req.fornecedor().razaoSocial());

        var itens = req.itens().stream().map(item -> {
            var insumoEntrada = (item.insumo().id() != null)
                    ? ProcessarNotaFiscalUseCase.InsumoEntrada.existente(item.insumo().id())
                    : ProcessarNotaFiscalUseCase.InsumoEntrada.novo(
                            item.insumo().codigo(), item.insumo().nome(), item.insumo().unidadeBaseId());
            return new ProcessarNotaFiscalUseCase.ItemEntrada(
                    insumoEntrada, item.codigoFornecedor(), item.descricaoOrigem(),
                    item.quantidade(), item.unidadeMedidaId(),
                    item.valorUnitario(), item.valorTotal(),
                    item.lote(), item.dataValidade());
        }).toList();

        var cmd = new ProcessarNotaFiscalUseCase.Comando(
                req.filialId(), req.usuarioId(), fornecedorEntrada,
                req.numero(), req.serie(), req.chaveNfe(), req.dataEmissao(),
                req.valorTotal(), req.observacao(), itens);

        return NotaFiscalDto.Response.from(processar.execute(cmd));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR', 'CONSULTA')")
    public List<NotaFiscalDto.Response> list(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(required = false) UUID fornecedorId,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String chaveNfe,
            @RequestParam(required = false) Instant emissaoDe,
            @RequestParam(required = false) Instant emissaoAte,
            @RequestParam(required = false) Instant lancamentoDe,
            @RequestParam(required = false) Instant lancamentoAte,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filtros = new NotaFiscalRepository.Filtros(
                filialId, fornecedorId, numero, chaveNfe,
                emissaoDe, emissaoAte, lancamentoDe, lancamentoAte);
        return listar.execute(filtros, page, size).stream()
                .map(NotaFiscalDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR', 'CONSULTA')")
    public NotaFiscalDto.Response getById(@PathVariable UUID id) {
        return NotaFiscalDto.Response.from(buscar.execute(id));
    }
}
