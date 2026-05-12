import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Trash2 } from 'lucide-react';
import { toast } from 'sonner';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { toastError } from '@/lib/toastError';

import { type DeParaItem, apagarDePara, listarDeParas } from './api';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  fornecedorId: string | null;
  fornecedorNome: string;
}

export function FornecedorDeParaDialog({ open, onOpenChange, fornecedorId, fornecedorNome }: Props) {
  const queryClient = useQueryClient();

  const deparasQuery = useQuery({
    queryKey: ['fornecedor-de-para', fornecedorId],
    queryFn: () => listarDeParas(fornecedorId!),
    enabled: open && !!fornecedorId,
  });

  const apagarMutation = useMutation({
    mutationFn: (codigo: string) => apagarDePara(fornecedorId!, codigo),
    onSuccess: () => {
      toast.success('Mapeamento removido. Próxima nota com esse código pedirá decisão de novo.');
      queryClient.invalidateQueries({ queryKey: ['fornecedor-de-para', fornecedorId] });
    },
    onError: (e) => toastError('Não foi possível remover o mapeamento', e),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Mapeamentos NF-e — {fornecedorNome}</DialogTitle>
          <DialogDescription>
            Códigos do fornecedor (cProd) já aprendidos. Toda nova nota deste fornecedor
            consulta esta tabela antes de sugerir criar insumo. Apague mapeamentos errados
            pra forçar uma nova decisão na próxima importação.
          </DialogDescription>
        </DialogHeader>

        {deparasQuery.isLoading && (
          <div className="py-6 text-center text-sm text-muted-foreground">Carregando…</div>
        )}

        {deparasQuery.data && deparasQuery.data.length === 0 && (
          <div className="py-6 text-center text-sm text-muted-foreground">
            Nenhum mapeamento aprendido ainda. Vai ser gerado automaticamente quando a primeira
            nota deste fornecedor for lançada.
          </div>
        )}

        {deparasQuery.data && deparasQuery.data.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b text-left text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th className="px-2 py-2 w-[140px]">cProd (fornecedor)</th>
                  <th className="px-2 py-2">Insumo vinculado</th>
                  <th className="px-2 py-2 w-[140px]">Último uso</th>
                  <th className="px-2 py-2 w-[80px]"></th>
                </tr>
              </thead>
              <tbody>
                {deparasQuery.data.map((dp: DeParaItem) => (
                  <tr key={dp.codigoFornecedor} className="border-b last:border-0">
                    <td className="px-2 py-2 font-mono text-xs">{dp.codigoFornecedor}</td>
                    <td className="px-2 py-2">
                      <div>{dp.insumoNome}</div>
                      {dp.insumoCodigo && (
                        <code className="text-[10px] text-muted-foreground">{dp.insumoCodigo}</code>
                      )}
                    </td>
                    <td className="px-2 py-2 text-xs text-muted-foreground">
                      {new Date(dp.lastUsedAt).toLocaleDateString('pt-BR')}
                    </td>
                    <td className="px-2 py-2 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={apagarMutation.isPending}
                        onClick={() => {
                          if (confirm(`Remover o mapeamento cProd '${dp.codigoFornecedor}' → '${dp.insumoNome}'?`)) {
                            apagarMutation.mutate(dp.codigoFornecedor);
                          }
                        }}
                      >
                        <Trash2 className="h-4 w-4" /> Apagar
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Fechar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
