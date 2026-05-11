import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Receipt, Search } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PageHeader } from '@/components/PageHeader';
import { Card, CardContent } from '@/components/ui/card';
import { toastError } from '@/lib/toastError';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import { useAuthStore } from '@/features/auth/store';
import { listarProdutos } from '@/features/cadastros/produtos/api';

import { registrarVenda } from './api';
import { ConfirmarVendaDialog } from './ConfirmarVendaDialog';

export function VendasPage() {
  const queryClient = useQueryClient();
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const usuarioId = useAuthStore((s) => s.user?.id);
  const [busca, setBusca] = useState('');
  const [quantidades, setQuantidades] = useState<Record<string, string>>({});
  const [confirmacao, setConfirmacao] = useState<
    | { produtoId: string; produtoNome: string; quantidade: number }
    | null
  >(null);

  const produtosQuery = useQuery({
    queryKey: ['produtos', { ativo: true, all: true }],
    queryFn: () => listarProdutos({ ativo: true }),
  });

  const produtosFiltrados = useMemo(() => {
    const lista = produtosQuery.data ?? [];
    const q = busca.trim().toLowerCase();
    if (!q) return lista;
    return lista.filter(
      (p) => p.nome.toLowerCase().includes(q) || p.codigo.toLowerCase().includes(q),
    );
  }, [produtosQuery.data, busca]);

  const mutation = useMutation({
    mutationFn: registrarVenda,
    onSuccess: (data, variables) => {
      const itens = data.itens?.length ?? 0;
      const negativo = data.gerouNegativo;
      toast.success('Venda registrada', {
        description: `${itens} ${itens === 1 ? 'insumo debitado' : 'insumos debitados'} via ficha técnica${negativo ? ' (atenção: saldo ficou negativo em algum lote)' : ''}.`,
      });
      // Limpa quantidade do produto vendido e invalida saldos.
      setQuantidades((prev) => ({ ...prev, [variables.produtoVendavelId]: '' }));
      setConfirmacao(null);
      queryClient.invalidateQueries({ queryKey: ['posicao'] });
      queryClient.invalidateQueries({ queryKey: ['ruptura'] });
      queryClient.invalidateQueries({ queryKey: ['mov-historico'] });
    },
    onError: (e) => toastError('Não foi possível registrar a venda', e),
  });

  function handleVender(produtoId: string, produtoNome: string) {
    if (!filialId) {
      toast.error('Selecione uma filial no topo');
      return;
    }
    if (!usuarioId) {
      toast.error('Sessão expirada — relogue');
      return;
    }
    const qStr = quantidades[produtoId];
    const q = Number(qStr);
    if (!qStr || isNaN(q) || q <= 0) {
      toast.error('Informe uma quantidade > 0');
      return;
    }
    setConfirmacao({ produtoId, produtoNome, quantidade: q });
  }

  function confirmarVenda() {
    if (!confirmacao || !filialId || !usuarioId) return;
    mutation.mutate({
      produtoVendavelId: confirmacao.produtoId,
      filialId,
      usuarioId,
      quantidadeVendida: confirmacao.quantidade,
    });
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Vendas"
        description="Registre vendas de itens do cardápio. A baixa de produtos do estoque é feita automaticamente via ficha técnica vigente (FEFO)."
      />

      <div className="rounded-md border bg-card p-4">
        <Label htmlFor="busca-venda">Buscar item do cardápio</Label>
        <div className="relative mt-1.5">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            id="busca-venda"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Nome ou código…"
            className="pl-9"
          />
        </div>
      </div>

      {!filialId && (
        <Card>
          <CardContent className="py-6 text-center text-sm text-muted-foreground">
            Selecione uma filial no topo da página para registrar vendas.
          </CardContent>
        </Card>
      )}

      {filialId && produtosFiltrados.length === 0 && !produtosQuery.isLoading && (
        <Card>
          <CardContent className="py-6 text-center text-sm text-muted-foreground">
            {busca
              ? `Nenhum item de cardápio bate com "${busca}".`
              : 'Nenhum item de cardápio cadastrado. Cadastre em Cardápio.'}
          </CardContent>
        </Card>
      )}

      <ConfirmarVendaDialog
        open={!!confirmacao}
        onOpenChange={(open) => {
          if (!open && !mutation.isPending) setConfirmacao(null);
        }}
        produtoVendavelId={confirmacao?.produtoId ?? null}
        produtoNome={confirmacao?.produtoNome ?? ''}
        filialId={filialId ?? ''}
        quantidade={confirmacao?.quantidade ?? 0}
        confirmando={mutation.isPending}
        onConfirm={confirmarVenda}
      />

      {filialId && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {produtosFiltrados.map((p) => (
            <Card key={p.id}>
              <CardContent className="p-4 space-y-3">
                <div>
                  <code className="text-[10px] uppercase tracking-wide text-muted-foreground">
                    {p.codigo}
                  </code>
                  <h3 className="font-display text-lg leading-tight">{p.nome}</h3>
                  <p className="text-xs text-muted-foreground">{p.categoria}</p>
                </div>
                <div className="flex items-end gap-2">
                  <div className="flex-1 space-y-1">
                    <Label htmlFor={`qty-${p.id}`} className="text-xs">
                      Quantidade
                    </Label>
                    <Input
                      id={`qty-${p.id}`}
                      type="number"
                      min="0"
                      step="1"
                      value={quantidades[p.id] ?? ''}
                      onChange={(e) =>
                        setQuantidades((prev) => ({ ...prev, [p.id]: e.target.value }))
                      }
                      placeholder="1"
                    />
                  </div>
                  <Button
                    onClick={() => handleVender(p.id, p.nome)}
                    disabled={
                      mutation.isPending && mutation.variables?.produtoVendavelId === p.id
                    }
                  >
                    <Receipt className="h-4 w-4" />
                    {mutation.isPending && mutation.variables?.produtoVendavelId === p.id
                      ? 'Vendendo…'
                      : 'Vender'}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
