package com.nonnas.alerts.domain;

import com.nonnas.alerts.application.ports.AlertaConfigRepository;
import com.nonnas.alerts.application.ports.AlertaDisparadoRepository;
import com.nonnas.catalog.application.ports.InsumoFilialRepository;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.LoteId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Coração do módulo. Avalia configs vs estado atual de saldo/lotes para
 * disparar e auto-resolver alertas. Lógica:
 *
 * <ol>
 *   <li><b>Estoque</b> ({@code RUPTURA}, {@code ESTOQUE_MINIMO_*}) — invocado
 *       pelo {@code MovimentacaoAlertaListener} após cada movimentação.
 *       Para o par {@code (insumo, filial)}, encontra a config mais
 *       específica de cada tipo e checa o threshold contra o saldo atual.
 *       Auto-resolve alertas ativos quando o saldo voltar ao normal.</li>
 *   <li><b>Vencimento</b> ({@code VENCIMENTO_PROXIMO_DIAS}) — invocado pelo
 *       job {@code @Scheduled} diário. Para cada config ativa, varre lotes
 *       com {@code data_validade ≤ hoje + threshold} e {@code saldo > 0}.
 *       Idempotente via partial unique index do schema. Auto-resolve quando
 *       o lote zera (chamado também pelo listener de movimentação).</li>
 * </ol>
 *
 * Sem Spring — domain service. Recebe portas via construtor.
 */
public final class AvaliadorAlertasService {

    private static final ZoneId FUSO_BR = ZoneId.of("America/Sao_Paulo");

    private final AlertaConfigRepository configRepo;
    private final AlertaDisparadoRepository disparadoRepo;
    private final SaldoLoteRepository saldoRepo;
    private final InsumoFilialRepository insumoFilialRepo;
    private final Clock clock;

    public AvaliadorAlertasService(AlertaConfigRepository configRepo,
                                   AlertaDisparadoRepository disparadoRepo,
                                   SaldoLoteRepository saldoRepo,
                                   InsumoFilialRepository insumoFilialRepo,
                                   Clock clock) {
        this.configRepo = configRepo;
        this.disparadoRepo = disparadoRepo;
        this.saldoRepo = saldoRepo;
        this.insumoFilialRepo = insumoFilialRepo;
        this.clock = clock;
    }

    /**
     * Avaliação reativa de alertas de estoque para o par {@code (insumo, filial)}.
     * Chamado após cada movimentação. Dispara novos alertas e auto-resolve
     * existentes conforme o saldo atual.
     */
    public void avaliarEstoque(UUID insumoId, UUID filialId) {
        BigDecimal saldoAtual = saldoRepo.somarPorInsumoEFilial(insumoId, filialId);

        avaliarPorTipo(TipoAlerta.RUPTURA, insumoId, filialId, saldoAtual);
        avaliarPorTipo(TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL, insumoId, filialId, saldoAtual);
        avaliarPorTipo(TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO, insumoId, filialId, saldoAtual);
    }

    /**
     * Auto-resolução de alertas de vencimento quando lotes monitorados
     * tiveram saldo zerado. Chamado após movimentações que afetam lotes
     * específicos.
     */
    public void avaliarLotesVencimento(List<UUID> loteIds, UUID filialId) {
        for (UUID loteId : loteIds) {
            Optional<com.nonnas.inventory.domain.SaldoLote> saldo =
                    saldoRepo.findById(LoteId.of(loteId), filialId);
            BigDecimal qtd = saldo.map(s -> s.quantidadeBase()).orElse(BigDecimal.ZERO);
            if (qtd.signum() <= 0) {
                for (var ativo : disparadoRepo.findAtivosPorLote(loteId)) {
                    ativo.resolverAuto(clock.instant());
                    disparadoRepo.save(ativo);
                }
            }
        }
    }

    /**
     * Job diário: varre lotes vencendo nos próximos N dias (N varia por config)
     * e dispara alertas. Idempotente — partial unique index do schema previne
     * duplicação de alertas ATIVOS para o mesmo {@code (config, lote)}.
     */
    public int avaliarVencimentos() {
        List<AlertaConfig> configs = configRepo.findAtivasPorTipo(TipoAlerta.VENCIMENTO_PROXIMO_DIAS);
        if (configs.isEmpty()) return 0;

        int diasMax = configs.stream()
                .mapToInt(c -> c.thresholdOpt().orElseThrow().intValue())
                .max().orElse(0);
        LocalDate hoje = LocalDate.now(clock.withZone(FUSO_BR));
        LocalDate ate = hoje.plusDays(diasMax);
        var candidatos = saldoRepo.findLotesVencendoComSaldoAte(ate);
        int disparados = 0;

        for (var candidato : candidatos) {
            int diasRestantes = (int) java.time.temporal.ChronoUnit.DAYS.between(hoje, candidato.dataValidade());
            // Para cada config aplicável, decide se dispara para este lote
            AlertaConfig escolhida = melhorMatch(
                    configRepo.findAtivasParaEscopo(TipoAlerta.VENCIMENTO_PROXIMO_DIAS,
                            candidato.insumoId(), candidato.filialId()),
                    candidato.insumoId(), candidato.filialId());
            if (escolhida == null) continue;
            int dias = escolhida.thresholdOpt().orElseThrow().intValue();
            if (diasRestantes > dias) continue;

            // Idempotência: já existe ATIVO para (config, lote)?
            if (disparadoRepo.findAtivoPorLote(escolhida.id(), candidato.loteId().value()).isPresent()) {
                continue;
            }

            String detalhe = "Lote vence em %d dia(s) — saldo %s".formatted(
                    diasRestantes, candidato.saldoBase().toPlainString());
            var disparado = AlertaDisparado.disparar(
                    escolhida.id(), TipoAlerta.VENCIMENTO_PROXIMO_DIAS,
                    candidato.insumoId(), candidato.filialId(), candidato.loteId().value(),
                    candidato.saldoBase(), detalhe, clock.instant());
            disparadoRepo.salvarNovo(disparado);
            disparados++;
        }
        return disparados;
    }

    private void avaliarPorTipo(TipoAlerta tipo, UUID insumoId, UUID filialId, BigDecimal saldoAtual) {
        List<AlertaConfig> aplicaveis = configRepo.findAtivasParaEscopo(tipo, insumoId, filialId);
        AlertaConfig escolhida = melhorMatch(aplicaveis, insumoId, filialId);
        if (escolhida == null) return;

        boolean cruzouThreshold = checarThreshold(escolhida, insumoId, filialId, saldoAtual);

        Optional<AlertaDisparado> jaAtivo =
                disparadoRepo.findAtivoSemLote(escolhida.id(), insumoId, filialId);

        if (cruzouThreshold && jaAtivo.isEmpty()) {
            String detalhe = montarDetalhe(escolhida, saldoAtual);
            var d = AlertaDisparado.disparar(
                    escolhida.id(), tipo, insumoId, filialId, null,
                    saldoAtual, detalhe, clock.instant());
            disparadoRepo.salvarNovo(d);
        } else if (!cruzouThreshold && jaAtivo.isPresent()) {
            // Auto-resolução
            var ativo = jaAtivo.get();
            ativo.resolverAuto(clock.instant());
            disparadoRepo.save(ativo);
        }
    }

    /**
     * Match mais específico primeiro. Empate: maior {@code prioridade}.
     * Retorna {@code null} se nenhuma config aplicável.
     */
    AlertaConfig melhorMatch(List<AlertaConfig> aplicaveis, UUID insumoId, UUID filialId) {
        return aplicaveis.stream()
                .filter(c -> c.insumoIdOpt().map(id -> id.equals(insumoId)).orElse(true))
                .filter(c -> c.filialIdOpt().map(id -> id.equals(filialId)).orElse(true))
                .max(Comparator
                        .comparingInt(AlertaConfig::especificidade)
                        .thenComparingInt(AlertaConfig::prioridade))
                .orElse(null);
    }

    private boolean checarThreshold(AlertaConfig config, UUID insumoId, UUID filialId, BigDecimal saldoAtual) {
        return switch (config.tipo()) {
            case RUPTURA -> saldoAtual.signum() <= 0;
            case ESTOQUE_MINIMO_ABSOLUTO ->
                    saldoAtual.compareTo(config.thresholdOpt().orElseThrow()) < 0;
            case ESTOQUE_MINIMO_PERCENTUAL -> {
                Optional<BigDecimal> max = insumoFilialRepo
                        .findByInsumoEFilial(InsumoId.of(insumoId), filialId)
                        .flatMap(i -> i.estoqueMaximo());
                if (max.isEmpty() || max.get().signum() <= 0) yield false;
                BigDecimal percentual = saldoAtual.multiply(new BigDecimal("100"))
                        .divide(max.get(), 4, RoundingMode.HALF_UP);
                yield percentual.compareTo(config.thresholdOpt().orElseThrow()) < 0;
            }
            case VENCIMENTO_PROXIMO_DIAS -> false;  // não avaliado por saldo
        };
    }

    private String montarDetalhe(AlertaConfig config, BigDecimal saldoAtual) {
        return switch (config.tipo()) {
            case RUPTURA -> "Saldo zerado ou negativo: " + saldoAtual.toPlainString();
            case ESTOQUE_MINIMO_ABSOLUTO -> "Saldo %s abaixo do mínimo %s"
                    .formatted(saldoAtual.toPlainString(), config.thresholdOpt().orElseThrow().toPlainString());
            case ESTOQUE_MINIMO_PERCENTUAL -> "Saldo abaixo de %s%% do estoque máximo"
                    .formatted(config.thresholdOpt().orElseThrow().toPlainString());
            default -> "";
        };
    }
}
