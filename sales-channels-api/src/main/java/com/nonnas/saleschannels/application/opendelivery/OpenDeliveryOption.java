package com.nonnas.saleschannels.application.opendelivery;

import java.math.BigDecimal;

/**
 * Customização de um item (Open Delivery v1.0.1 — {@code item.options[]}).
 *
 * <p>Para o POC só capturamos identificação e quantidade; o impacto em
 * estoque das customizações virá com receitas variantes (fora de escopo
 * do MVP de canais).
 */
public record OpenDeliveryOption(
        String externalCode,
        String name,
        BigDecimal quantity,
        OpenDeliveryUnit unit,
        OpenDeliveryPrice unitPrice
) {}
