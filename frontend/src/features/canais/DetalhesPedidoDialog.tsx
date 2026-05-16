import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

import { CANAL_LABEL, STATUS_LABEL, type PedidoCanal } from './api';

function fmtMoeda(valor: number, moeda: string) {
  try {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: moeda || 'BRL',
    }).format(valor);
  } catch {
    return `${moeda} ${valor.toFixed(2)}`;
  }
}

function fmtDate(iso: string | null): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('pt-BR');
  } catch {
    return iso;
  }
}

interface Props {
  pedido: PedidoCanal | null;
  onClose: () => void;
}

export function DetalhesPedidoDialog({ pedido, onClose }: Props) {
  return (
    <Dialog open={pedido !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-2xl">
        {pedido && (
          <>
            <DialogHeader>
              <DialogTitle>
                Pedido {pedido.displayId ?? pedido.pedidoExternoId}
              </DialogTitle>
              <DialogDescription>
                {CANAL_LABEL[pedido.canalTipo]} · {STATUS_LABEL[pedido.status]}
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <span className="text-xs text-muted-foreground">ID externo</span>
                  <p className="font-mono text-xs">{pedido.pedidoExternoId}</p>
                </div>
                <div>
                  <span className="text-xs text-muted-foreground">Cliente</span>
                  <p>
                    {pedido.clienteNome ?? '—'}
                    {pedido.clienteTelefone ? ` · ${pedido.clienteTelefone}` : ''}
                  </p>
                </div>
                <div>
                  <span className="text-xs text-muted-foreground">Recebido em</span>
                  <p>{fmtDate(pedido.recebidoEm)}</p>
                </div>
                <div>
                  <span className="text-xs text-muted-foreground">Processado em</span>
                  <p>{fmtDate(pedido.processadoEm)}</p>
                </div>
                {pedido.concluidoEm && (
                  <div>
                    <span className="text-xs text-muted-foreground">Concluído em</span>
                    <p>{fmtDate(pedido.concluidoEm)}</p>
                  </div>
                )}
                {pedido.canceladoEm && (
                  <div>
                    <span className="text-xs text-muted-foreground">Cancelado em</span>
                    <p>{fmtDate(pedido.canceladoEm)}</p>
                  </div>
                )}
              </div>

              {pedido.erroProcessamento && (
                <div className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-destructive text-sm">
                  {pedido.erroProcessamento}
                </div>
              )}

              <div>
                <h4 className="mb-2 text-sm font-semibold">Itens</h4>
                <div className="overflow-x-auto rounded-md border">
                  <table className="w-full text-xs">
                    <thead className="bg-muted/50">
                      <tr>
                        <th className="px-3 py-2 text-left">Cód. externo</th>
                        <th className="px-3 py-2 text-left">Nome</th>
                        <th className="px-3 py-2 text-right">Qtd</th>
                        <th className="px-3 py-2 text-left">Unid.</th>
                        <th className="px-3 py-2 text-right">Unitário</th>
                        <th className="px-3 py-2 text-right">Total</th>
                        <th className="px-3 py-2 text-center">Mapeado?</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pedido.itens.map((item) => (
                        <tr key={item.sequencia} className="border-t">
                          <td className="px-3 py-2 font-mono">
                            {item.externalCode ?? '—'}
                          </td>
                          <td className="px-3 py-2">
                            {item.nome}
                            {item.observacao && (
                              <div className="text-muted-foreground">{item.observacao}</div>
                            )}
                          </td>
                          <td className="px-3 py-2 text-right">{item.quantidade}</td>
                          <td className="px-3 py-2">{item.unidade}</td>
                          <td className="px-3 py-2 text-right">
                            {fmtMoeda(item.precoUnitario, pedido.moeda)}
                          </td>
                          <td className="px-3 py-2 text-right">
                            {fmtMoeda(item.precoTotal, pedido.moeda)}
                          </td>
                          <td className="px-3 py-2 text-center">
                            {item.produtoVendavelId ? (
                              <span className="text-emerald-700">✓</span>
                            ) : (
                              <span className="text-amber-700">—</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr className="border-t bg-muted/30 font-medium">
                        <td colSpan={5} className="px-3 py-2 text-right">
                          Total
                        </td>
                        <td className="px-3 py-2 text-right">
                          {fmtMoeda(pedido.valorTotal, pedido.moeda)}
                        </td>
                        <td></td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </div>

              {pedido.movimentacaoId && (
                <p className="text-xs text-muted-foreground">
                  Movimentação de saída registrada: <code>{pedido.movimentacaoId}</code>
                </p>
              )}
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
