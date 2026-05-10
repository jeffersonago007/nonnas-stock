package com.nonnas.inventory.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lote de um insumo. Imutável após criação.
 *
 * <p>Dois regimes (T-LOT-03 / adendo lote opcional):
 * <ul>
 *   <li>{@link TipoLote#RASTREADO}: lote físico real recebido em uma entrada.
 *       Tem numero, datas, fornecedor, NF e valor unitário. Saída via FEFO.</li>
 *   <li>{@link TipoLote#AGREGADOR}: lote único por insumo que serve de balde
 *       para o saldo agregado quando o insumo não controla validade. Sem
 *       numero, sem datas, sem NF, sem fornecedor, valor unitário = 0.
 *       Criado lazy via {@code BuscarOuCriarLoteAgregadorUseCase}.</li>
 * </ul>
 */
public record Lote(
        LoteId id,
        UUID insumoId,
        TipoLote tipo,
        UUID fornecedorId,
        UUID notaFiscalId,
        String numeroLote,
        LocalDate dataFabricacao,
        LocalDate dataValidade,
        BigDecimal valorUnitario,
        Instant createdAt
) {
    public Lote {
        Objects.requireNonNull(id);
        Objects.requireNonNull(insumoId);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(valorUnitario);
        Objects.requireNonNull(createdAt);

        if (tipo == TipoLote.RASTREADO) {
            // Numero é obrigatório para lote rastreado — historicamente sempre
            // foi (constraint NOT NULL caiu na V020 só pra acomodar AGREGADOR).
            if (numeroLote == null || numeroLote.isBlank()) {
                throw new ValidationException("Número do lote é obrigatório para lote rastreado");
            }
        } else {
            // AGREGADOR: campos exclusivamente de rastreio devem ser vazios.
            // Schema CHECK reforça, mas validamos aqui também para falhar cedo.
            if (numeroLote != null) {
                throw new ValidationException("Lote agregador não pode ter número");
            }
            if (dataFabricacao != null) {
                throw new ValidationException("Lote agregador não pode ter data de fabricação");
            }
            if (dataValidade != null) {
                throw new ValidationException("Lote agregador não pode ter data de validade");
            }
        }
        if (valorUnitario.signum() < 0) {
            throw new ValidationException("Valor unitário não pode ser negativo");
        }
        if (dataValidade != null && dataFabricacao != null && dataValidade.isBefore(dataFabricacao)) {
            throw new ValidationException("Validade não pode ser anterior à fabricação");
        }
    }

    /**
     * Cria lote rastreado — caminho original. Validade pode ser null se o
     * insumo não controla validade (ainda assim cria rastreado para preservar
     * o vínculo com fornecedor/NF), mas o caller normalmente já decidiu por
     * AGREGADOR antes de chamar este factory.
     */
    public static Lote novoRastreado(UUID insumoId, UUID fornecedorId, UUID notaFiscalId,
                                     String numeroLote, LocalDate fabricacao, LocalDate validade,
                                     BigDecimal valorUnitario, Instant agora) {
        return new Lote(LoteId.generate(), insumoId, TipoLote.RASTREADO, fornecedorId, notaFiscalId,
                numeroLote != null ? numeroLote.trim() : null,
                fabricacao, validade, valorUnitario, agora);
    }

    /**
     * Cria o lote agregador único do insumo. Sem fornecedor/NF/numero/datas.
     * Valor unitário fica 0 — atualizações de custo no agregador são
     * decisão futura (precisaria custo médio ponderado, fora do escopo atual).
     */
    public static Lote novoAgregador(UUID insumoId, Instant agora) {
        return new Lote(LoteId.generate(), insumoId, TipoLote.AGREGADOR,
                null, null, null, null, null, BigDecimal.ZERO, agora);
    }

    public Optional<UUID> fornecedorIdOpt() { return Optional.ofNullable(fornecedorId); }
    public Optional<UUID> notaFiscalIdOpt() { return Optional.ofNullable(notaFiscalId); }
    public Optional<String> numeroLoteOpt() { return Optional.ofNullable(numeroLote); }
    public Optional<LocalDate> dataFabricacaoOpt() { return Optional.ofNullable(dataFabricacao); }
    public Optional<LocalDate> dataValidadeOpt() { return Optional.ofNullable(dataValidade); }
}
