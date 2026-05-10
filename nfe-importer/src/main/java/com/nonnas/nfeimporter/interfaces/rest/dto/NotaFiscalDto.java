package com.nonnas.nfeimporter.interfaces.rest.dto;

import com.nonnas.nfeimporter.domain.EmitenteLido;
import com.nonnas.nfeimporter.domain.ItemLido;
import com.nonnas.nfeimporter.domain.NotaFiscalLida;
import com.nonnas.operations.domain.ItemNotaFiscal;
import com.nonnas.operations.domain.NotaFiscal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class NotaFiscalDto {

    /** Resposta do endpoint /preview-xml — XML parseado, ainda não persistido. */
    public record PreviewResponse(
            String chaveAcesso,
            String numero,
            String serie,
            Instant dataEmissao,
            BigDecimal valorTotal,
            EmitenteResumo emitente,
            List<ItemPreview> itens
    ) {
        public static PreviewResponse from(NotaFiscalLida nf) {
            return new PreviewResponse(
                    nf.chaveAcesso(), nf.numero(), nf.serie(),
                    nf.dataEmissao().toInstant(), nf.valorTotal(),
                    EmitenteResumo.from(nf.emitente()),
                    nf.itens().stream().map(ItemPreview::from).toList());
        }
    }

    public record EmitenteResumo(String cnpj, String razaoSocial, String nomeFantasia,
                                 String inscricaoEstadual) {
        static EmitenteResumo from(EmitenteLido e) {
            return new EmitenteResumo(e.cnpj(), e.razaoSocial(), e.nomeFantasia(), e.inscricaoEstadual());
        }
    }

    public record ItemPreview(int numero, String codigoFornecedor, String descricao, String ncm,
                              String unidadeComercial, BigDecimal quantidade,
                              BigDecimal valorUnitario, BigDecimal valorTotal) {
        static ItemPreview from(ItemLido i) {
            return new ItemPreview(i.numero(), i.codigoFornecedor(), i.descricao(),
                    i.ncm(), i.unidadeComercial(), i.quantidade(),
                    i.valorUnitario(), i.valorTotal());
        }
    }

    /** Payload do endpoint /lancar — fornecedor e itens já editados pelo operador. */
    public record LancarRequest(
            @NotNull UUID filialId,
            @NotNull UUID usuarioId,
            @NotNull FornecedorRef fornecedor,
            @NotBlank String numero,
            @NotBlank String serie,
            String chaveNfe,
            @NotNull Instant dataEmissao,
            @NotNull @PositiveOrZero BigDecimal valorTotal,
            String observacao,
            @NotEmpty @Valid List<ItemRequest> itens
    ) {}

    public record FornecedorRef(UUID id, String cnpj, String razaoSocial) {}

    public record InsumoRef(UUID id, String codigo, String nome, UUID unidadeBaseId) {}

    public record ItemRequest(
            @NotNull @Valid InsumoRef insumo,
            String codigoFornecedor,
            @NotBlank String descricaoOrigem,
            @NotNull @Positive BigDecimal quantidade,
            @NotNull UUID unidadeMedidaId,
            @NotNull @PositiveOrZero BigDecimal valorUnitario,
            BigDecimal valorTotal,
            String lote,
            LocalDate dataValidade
    ) {}

    /** Resposta de criação/consulta. */
    public record Response(
            UUID id,
            UUID fornecedorId,
            UUID filialId,
            String numero,
            String serie,
            String chaveNfe,
            Instant dataEmissao,
            Instant dataLancamento,
            BigDecimal valorTotal,
            String observacao,
            UUID createdByUsuarioId,
            UUID movimentacaoEntradaId,
            List<ItemResponse> itens
    ) {
        public static Response from(NotaFiscal n) {
            return new Response(
                    n.id().value(), n.fornecedorId(), n.filialId(),
                    n.numero(), n.serie(), n.chaveNfeOpt().orElse(null),
                    n.dataEmissao(), n.dataLancamento(),
                    n.valorTotal(), n.observacaoOpt().orElse(null),
                    n.createdByUsuarioId(), n.movimentacaoEntradaId(),
                    n.itens().stream().map(ItemResponse::from).toList());
        }
    }

    public record ItemResponse(
            UUID id, UUID insumoId, String codigoFornecedor, String descricaoOrigem,
            BigDecimal quantidade, UUID unidadeMedidaId,
            BigDecimal valorUnitario, BigDecimal valorTotal,
            String lote, LocalDate dataValidade
    ) {
        static ItemResponse from(ItemNotaFiscal i) {
            return new ItemResponse(
                    i.id(), i.insumoId(), i.codigoFornecedorOpt().orElse(null), i.descricaoOrigem(),
                    i.quantidade(), i.unidadeMedidaId(),
                    i.valorUnitario(), i.valorTotal(),
                    i.loteOpt().orElse(null), i.dataValidadeOpt().orElse(null));
        }
    }

    private NotaFiscalDto() {}
}
