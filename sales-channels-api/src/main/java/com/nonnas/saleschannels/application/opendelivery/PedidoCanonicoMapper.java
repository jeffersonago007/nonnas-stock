package com.nonnas.saleschannels.application.opendelivery;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanalId;
import com.nonnas.saleschannels.domain.ItemPedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Converte o contrato externo canônico {@link PedidoVendaCanonico} para
 * o aggregate de domínio {@link PedidoCanal} no estado RECEBIDO.
 *
 * <p>Stateless puro — não persiste, não chama I/O. Garante apenas:
 * <ul>
 *   <li>Sequência de itens é {@code item.index} se presente, senão a
 *       posição na lista (1-based).</li>
 *   <li>Valor total cai para {@code orderAmount}; quando ausente, soma
 *       os {@code totalPrice} dos itens.</li>
 *   <li>Moeda é a do {@code total.currency} se presente, senão do
 *       primeiro item, senão {@code "BRL"}.</li>
 * </ul>
 */
public final class PedidoCanonicoMapper {

    private PedidoCanonicoMapper() {}

    public static PedidoCanal paraDominio(PedidoVendaCanonico canonico,
                                          CanalTipo canalTipo,
                                          UUID filialId,
                                          CredencialCanalId credencialId,
                                          Instant agora) {
        List<ItemPedidoCanal> itens = mapearItens(canonico.items());
        BigDecimal total = resolverTotal(canonico, itens);
        String moeda = resolverMoeda(canonico);

        String clienteNome = canonico.customer() != null ? canonico.customer().name() : null;
        String clienteTelefone = canonico.customer() != null ? canonico.customer().phone() : null;

        return PedidoCanal.recebido(
                canalTipo,
                canonico.id(),
                canonico.displayId(),
                filialId,
                credencialId,
                total,
                moeda,
                clienteNome,
                clienteTelefone,
                itens,
                agora);
    }

    private static List<ItemPedidoCanal> mapearItens(List<OpenDeliveryItem> items) {
        return java.util.stream.IntStream.range(0, items.size())
                .mapToObj(i -> {
                    OpenDeliveryItem od = items.get(i);
                    int sequencia = od.index() > 0 ? od.index() : (i + 1);
                    return ItemPedidoCanal.novo(
                            sequencia,
                            od.externalCode(),
                            od.name(),
                            od.quantity(),
                            od.unit() != null ? od.unit().name() : "UN",
                            od.unitPrice() != null ? od.unitPrice().value() : BigDecimal.ZERO,
                            od.totalPrice() != null ? od.totalPrice().value() : BigDecimal.ZERO,
                            od.observations());
                })
                .toList();
    }

    private static BigDecimal resolverTotal(PedidoVendaCanonico canonico, List<ItemPedidoCanal> itens) {
        if (canonico.total() != null && canonico.total().orderAmount() != null) {
            return canonico.total().orderAmount();
        }
        return itens.stream()
                .map(ItemPedidoCanal::precoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String resolverMoeda(PedidoVendaCanonico canonico) {
        if (canonico.total() != null && canonico.total().currency() != null) {
            return canonico.total().currency();
        }
        for (OpenDeliveryItem item : canonico.items()) {
            if (item.unitPrice() != null && item.unitPrice().currency() != null) {
                return item.unitPrice().currency();
            }
        }
        return "BRL";
    }
}
