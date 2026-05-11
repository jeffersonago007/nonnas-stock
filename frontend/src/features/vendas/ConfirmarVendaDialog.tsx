import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, Receipt } from 'lucide-react';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

import { obterPreviewVenda, type PreviewVendaResposta } from './api';

interface ConfirmarVendaDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  produtoVendavelId: string | null;
  produtoNome: string;
  filialId: string;
  quantidade: number;
  confirmando: boolean;
  onConfirm: () => void;
}

function fmtQtd(n: number, unidade?: string) {
  const num = n.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 3 });
  return unidade ? `${num} ${unidade}` : num;
}

function diasAteValidade(iso: string): number {
  const hoje = new Date();
  hoje.setHours(0, 0, 0, 0);
  const validade = new Date(iso);
  validade.setHours(0, 0, 0, 0);
  return Math.round((validade.getTime() - hoje.getTime()) / (1000 * 60 * 60 * 24));
}

function rotuloValidade(iso: string): string {
  const dias = diasAteValidade(iso);
  if (dias < 0) return `vencido há ${Math.abs(dias)}d`;
  if (dias === 0) return 'vence hoje';
  if (dias === 1) return 'vence amanhã';
  return `vence em ${dias}d`;
}

export function ConfirmarVendaDialog({
  open,
  onOpenChange,
  produtoVendavelId,
  produtoNome,
  filialId,
  quantidade,
  confirmando,
  onConfirm,
}: ConfirmarVendaDialogProps) {
  const previewQuery = useQuery<PreviewVendaResposta>({
    queryKey: ['venda-preview', produtoVendavelId, filialId, quantidade],
    queryFn: () =>
      obterPreviewVenda({
        produtoVendavelId: produtoVendavelId!,
        filialId,
        quantidadeVendida: quantidade,
      }),
    enabled: open && !!produtoVendavelId && !!filialId && quantidade > 0,
    staleTime: 0,
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Confirmar venda</DialogTitle>
          <DialogDescription>
            {produtoNome} × {quantidade}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3 max-h-[60vh] overflow-y-auto">
          {previewQuery.isLoading && (
            <div className="py-6 text-center text-sm text-muted-foreground">Calculando baixa…</div>
          )}

          {previewQuery.isError && (
            <div className="py-6 text-center text-sm text-destructive">
              Não foi possível calcular o preview da venda.
            </div>
          )}

          {previewQuery.data && (
            <>
              {previewQuery.data.gerouNegativo && (
                <div className="flex items-start gap-2 rounded-md border border-amber-400 bg-amber-50 p-3 text-sm text-amber-900">
                  <AlertTriangle className="h-4 w-4 mt-0.5 shrink-0" />
                  <span>
                    Saldo insuficiente em pelo menos um insumo — a venda registra, mas o estoque ficará
                    negativo. Reponha o quanto antes.
                  </span>
                </div>
              )}

              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                Insumos a debitar via ficha técnica
              </p>

              <ul className="space-y-2">
                {previewQuery.data.itens.map((item) => (
                  <li key={item.insumoId} className="rounded-md border bg-card p-3">
                    <div className="flex items-baseline justify-between gap-2">
                      <span className="font-medium">{item.insumoNome}</span>
                      <span className="text-sm tabular-nums">
                        {fmtQtd(item.quantidadeBase, item.unidadeBase)}
                      </span>
                    </div>
                    {item.controlaValidade ? (
                      <ul className="mt-1.5 space-y-0.5 text-xs text-muted-foreground">
                        {item.lotes.map((l) => (
                          <li key={l.loteId} className="flex items-baseline gap-2">
                            <span>Lote {l.numero ?? '—'}</span>
                            {l.validade && (
                              <span
                                className={
                                  diasAteValidade(l.validade) <= 7
                                    ? 'text-amber-600 font-medium'
                                    : ''
                                }
                              >
                                ({rotuloValidade(l.validade)})
                              </span>
                            )}
                            <span className="ml-auto tabular-nums">
                              {fmtQtd(l.quantidade, item.unidadeBase)}
                            </span>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="mt-1 text-xs text-muted-foreground">
                        Saldo após:{' '}
                        <span
                          className={
                            item.saldoRestanteAposBaixa < 0
                              ? 'font-medium text-destructive'
                              : 'font-medium text-foreground'
                          }
                        >
                          {fmtQtd(item.saldoRestanteAposBaixa, item.unidadeBase)}
                        </span>
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={confirmando}>
            Cancelar
          </Button>
          <Button onClick={onConfirm} disabled={confirmando || previewQuery.isLoading}>
            <Receipt className="h-4 w-4" />
            {confirmando ? 'Registrando…' : 'Confirmar venda'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
