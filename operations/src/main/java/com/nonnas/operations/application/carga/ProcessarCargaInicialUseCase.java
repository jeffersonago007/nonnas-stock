package com.nonnas.operations.application.carga;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaMultiItemUseCase;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.operations.application.ports.CargaInicialRepository;
import com.nonnas.operations.domain.CargaInicial;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Processa carga inicial: idempotente via SHA-256 da planilha. Se o hash já
 * existe, retorna o registro anterior sem reprocessar. Caso contrário, gera
 * uma única movimentação ENTRADA_CARGA_INICIAL com N lotes via
 * {@link RegistrarEntradaMultiItemUseCase} e persiste o registro de carga.
 */
@Service
public class ProcessarCargaInicialUseCase {

    private static final String DOC_ORIGEM_TIPO = "CARGA_INICIAL";

    private final CargaInicialRepository cargaRepo;
    private final RegistrarEntradaMultiItemUseCase entradaMulti;
    private final Clock clock;

    public ProcessarCargaInicialUseCase(CargaInicialRepository cargaRepo,
                                        RegistrarEntradaMultiItemUseCase entradaMulti,
                                        Clock clock) {
        this.cargaRepo = cargaRepo;
        this.entradaMulti = entradaMulti;
        this.clock = clock;
    }

    @Transactional
    public CargaInicial execute(Comando cmd) {
        var existente = cargaRepo.findByHashPlanilha(cmd.hashPlanilha);
        if (existente.isPresent()) {
            return existente.get();
        }

        if (cmd.itens == null || cmd.itens.isEmpty()) {
            throw new ValidationException("Carga inicial deve ter ao menos um item");
        }

        var itensEntrada = cmd.itens.stream()
                .map(i -> new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                        i.insumoId, null, null, i.numeroLote,
                        i.dataFabricacao, i.dataValidade, i.valorUnitario,
                        i.unidadeId, i.quantidade, i.quantidade))
                .toList();

        UUID cargaTempId = UUID.randomUUID();  // doc_origem_id placeholder; persistimos o real depois
        entradaMulti.execute(new RegistrarEntradaMultiItemUseCase.Comando(
                cmd.filialId, cmd.solicitadoPor, TipoMovimentacao.ENTRADA_CARGA_INICIAL,
                DOC_ORIGEM_TIPO, cargaTempId, cmd.nomeArquivo, itensEntrada));

        CargaInicial registro = CargaInicial.novo(
                cmd.filialId, cmd.hashPlanilha, cmd.nomeArquivo,
                cmd.itens.size(), 0, cmd.solicitadoPor, clock.instant());
        return cargaRepo.save(registro);
    }

    public record Comando(
            UUID filialId,
            String hashPlanilha,
            String nomeArquivo,
            UUID solicitadoPor,
            List<ItemEntrada> itens
    ) {}

    public record ItemEntrada(
            UUID insumoId,
            UUID unidadeId,
            String numeroLote,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            LocalDate dataFabricacao,
            LocalDate dataValidade
    ) {}
}
