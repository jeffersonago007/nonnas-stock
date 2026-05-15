package com.nonnas.saleschannels.application.opendelivery;

import java.math.BigDecimal;
import java.util.List;

/**
 * Item de pedido — Open Delivery v1.0.1 (subset).
 *
 * <p>{@code externalCode} é a chave de resolução para nosso
 * {@code ProdutoVendavel} via tabela de-para (T-CANAL-04). Quando o canal
 * não envia {@code externalCode}, o operador resolve manualmente via UI.
 *
 * <p>{@code totalPrice} já vem computado pela origem para evitar
 * divergência de arredondamento entre canal e nosso sistema.
 */
public record OpenDeliveryItem(
        String id,
        int index,
        String externalCode,
        String name,
        BigDecimal quantity,
        OpenDeliveryUnit unit,
        OpenDeliveryPrice unitPrice,
        OpenDeliveryPrice totalPrice,
        String observations,
        List<OpenDeliveryOption> options
) {
    public OpenDeliveryItem {
        if (options == null) {
            options = List.of();
        }
    }
}
