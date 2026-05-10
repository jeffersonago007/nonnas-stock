package com.nonnas.inventory.domain;

/**
 * Tipo de lote — define o regime de rastreabilidade.
 *
 * <p>RASTREADO: lote físico real com numero e data de validade. Saída
 * obedece FEFO. Caminho original (T-LOT pré-adendo).
 *
 * <p>AGREGADOR: lote único por insumo que serve como "balde" do saldo
 * agregado quando o insumo não controla validade. Não tem número, não tem
 * datas, e o saldo por filial vive em {@code saldos_lotes(lote_id, filial_id)}
 * exatamente como um lote rastreado normal — a diferença é apenas que o
 * algoritmo de saída pula FEFO e vai direto pro agregador.
 *
 * <p>Schema (V020 inventory-core) garante invariantes de campos vazios
 * para AGREGADOR + unique partial index "um agregador por insumo".
 */
public enum TipoLote {
    RASTREADO,
    AGREGADOR
}
