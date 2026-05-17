package com.nonnas.saleschannels.application.opendelivery;

/**
 * Quem recebe o valor da taxa — campo {@code receivedBy} de {@code otherFees}
 * em Open Delivery v1.0.1. Mantido no contrato canônico para futura
 * conciliação financeira (não usado pela exibição operacional do POC).
 */
public enum OpenDeliveryFeeReceiver {
    MARKETPLACE,
    MERCHANT,
    LOGISTIC_SERVICES,
    OTHER
}
