package com.nonnas.saleschannels.application.ports;

import com.nonnas.saleschannels.application.opendelivery.EventoBruto;
import com.nonnas.saleschannels.application.opendelivery.PedidoVendaCanonico;
import com.nonnas.saleschannels.domain.CanalTipo;

import java.util.List;

/**
 * Contrato que cada canal de venda implementa para ser plugável no sistema.
 * Os adapters traduzem entre o payload nativo do canal e o subset Open
 * Delivery canônico ({@link PedidoVendaCanonico}).
 *
 * <p>Ciclo de uso (esperado em T-CANAL-04):
 * <ol>
 *   <li>{@link #consumirEventos(int)} é chamado por um scheduler de polling.</li>
 *   <li>Para cada {@link EventoBruto}, persiste-se um
 *       {@link com.nonnas.saleschannels.domain.EventoCanal} ({@code salvarSeNovo}
 *       garante idempotência).</li>
 *   <li>Se for {@code PEDIDO_CRIADO}, chama-se {@link #buscarPedido(String)}
 *       para obter o pedido canônico completo e materializa-se em
 *       {@link com.nonnas.saleschannels.domain.PedidoCanal}.</li>
 *   <li>{@link #acknowledgeEvento(String)} avisa o canal que o evento foi
 *       absorvido — sem isso, o canal continua reentregando.</li>
 *   <li>Após baixar estoque local (recipes/inventory-core), chama-se
 *       {@link #confirmarPedido(String)} (e depois despachar/concluir).</li>
 * </ol>
 *
 * <p>Implementações concretas:
 * <ul>
 *   <li>{@code IFoodAdapter} — sandbox iFood real (depende de credencial).</li>
 *   <li>{@code OpenDeliveryGenericoAdapter} — qualquer canal Open Delivery,
 *       ou mock server (Prism) consumindo a spec YAML pública.</li>
 *   <li>{@code KeetaAdapter}, {@code NoventaNoveFoodAdapter} — após
 *       integração com cada plataforma.</li>
 * </ul>
 */
public interface CanalAdapter {

    /** Tipo de canal que este adapter atende. */
    CanalTipo canal();

    /** Busca um pedido específico (após receber evento de criação). */
    PedidoVendaCanonico buscarPedido(String pedidoExternoId);

    /** Polling de eventos não-acknowledged. {@code max} limita o lote. */
    List<EventoBruto> consumirEventos(int max);

    /** Marca evento como recebido pelo nosso lado — canal para de reentregar. */
    void acknowledgeEvento(String eventIdExterno);

    /** Confirma para o canal que o pedido foi aceito (estoque ok). */
    void confirmarPedido(String pedidoExternoId);

    /** Marca como em preparo/despacho. */
    void despacharPedido(String pedidoExternoId);

    /** Marca como concluído / entregue. */
    void concluirPedido(String pedidoExternoId);

    /** Cancela o pedido com motivo livre (formato dependente do canal). */
    void cancelarPedido(String pedidoExternoId, String motivo);
}
