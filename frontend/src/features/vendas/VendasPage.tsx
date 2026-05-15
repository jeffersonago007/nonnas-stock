import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Receipt, Search, X } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PageHeader } from '@/components/PageHeader';
import { Card, CardContent } from '@/components/ui/card';
import { toastError } from '@/lib/toastError';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import { useAuthStore } from '@/features/auth/store';

import {
  type ItemCardapio,
  type OrigemItemCardapio,
  listarCardapio,
  registrarVenda,
  venderInsumoOrfao,
} from './api';
import { ConfirmarVendaDialog } from './ConfirmarVendaDialog';

interface SelecaoVenda {
  origem: OrigemItemCardapio;
  id: string;
  nome: string;
  quantidade: number;
}

export function VendasPage() {
  const queryClient = useQueryClient();
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const usuarioId = useAuthStore((s) => s.user?.id);
  const [buscaInput, setBuscaInput] = useState('');
  const [busca, setBusca] = useState('');
  const [quantidades, setQuantidades] = useState<Record<string, string>>({});
  const [confirmacao, setConfirmacao] = useState<SelecaoVenda | null>(null);

  function aplicarFiltros() {
    setBusca(buscaInput);
  }
  function limparFiltros() {
    setBuscaInput('');
    setBusca('');
  }

  const cardapioQuery = useQuery({
    queryKey: ['cardapio', filialId],
    queryFn: () => listarCardapio(filialId!),
    enabled: Boolean(filialId),
  });

  const itensFiltrados = useMemo(() => {
    const lista = cardapioQuery.data ?? [];
    const q = busca.trim().toLowerCase();
    if (!q) return lista;
    return lista.filter(
      (it) => it.nome.toLowerCase().includes(q) || it.codigo.toLowerCase().includes(q),
    );
  }, [cardapioQuery.data, busca]);

  function invalidarSaldosECardapio() {
    queryClient.invalidateQueries({ queryKey: ['cardapio'] });
    queryClient.invalidateQueries({ queryKey: ['posicao'] });
    queryClient.invalidateQueries({ queryKey: ['ruptura'] });
    queryClient.invalidateQueries({ queryKey: ['mov-historico'] });
    queryClient.invalidateQueries({ queryKey: ['produtos'] });
  }

  const vendaProdutoMutation = useMutation({
    mutationFn: registrarVenda,
    onSuccess: (data, variables) => {
      const itens = data.itens?.length ?? 0;
      const negativo = data.gerouNegativo;
      toast.success('Venda registrada', {
        description: `${itens} ${itens === 1 ? 'insumo debitado' : 'insumos debitados'}${negativo ? ' (atenção: saldo ficou negativo em algum lote)' : ''}.`,
      });
      setQuantidades((prev) => ({ ...prev, [variables.produtoVendavelId]: '' }));
      setConfirmacao(null);
      invalidarSaldosECardapio();
    },
    onError: (e) => toastError('Não foi possível registrar a venda', e),
  });

  const vendaInsumoMutation = useMutation({
    mutationFn: venderInsumoOrfao,
    onSuccess: (data, variables) => {
      toast.success('Venda registrada', {
        description: data.gerouNegativo
          ? 'Cadastrado no cardápio (atenção: saldo ficou negativo).'
          : 'Insumo cadastrado no cardápio e baixado do estoque.',
      });
      setQuantidades((prev) => ({ ...prev, [variables.insumoId]: '' }));
      setConfirmacao(null);
      invalidarSaldosECardapio();
    },
    onError: (e) => toastError('Não foi possível registrar a venda', e),
  });

  function handleVender(item: ItemCardapio) {
    if (!filialId) {
      toast.error('Selecione uma filial no topo');
      return;
    }
    if (!usuarioId) {
      toast.error('Sessão expirada — relogue');
      return;
    }
    // Campo vazio → assume 1 (o placeholder "1" sugere isso ao operador).
    const qStr = (quantidades[item.id] ?? '').trim() || '1';
    const q = Number(qStr);
    if (isNaN(q) || q <= 0) {
      toast.error('Informe uma quantidade > 0');
      return;
    }
    setConfirmacao({ origem: item.origem, id: item.id, nome: item.nome, quantidade: q });
  }

  function confirmarVenda() {
    if (!confirmacao || !filialId || !usuarioId) return;
    if (confirmacao.origem === 'INSUMO_ORFAO') {
      vendaInsumoMutation.mutate({
        insumoId: confirmacao.id,
        filialId,
        usuarioId,
        quantidadeVendida: confirmacao.quantidade,
      });
    } else {
      vendaProdutoMutation.mutate({
        produtoVendavelId: confirmacao.id,
        filialId,
        usuarioId,
        quantidadeVendida: confirmacao.quantidade,
      });
    }
  }

  const confirmando = vendaProdutoMutation.isPending || vendaInsumoMutation.isPending;
  const usaPreviewDeFicha =
    confirmacao?.origem === 'PRODUTO_FABRICADO' || confirmacao?.origem === 'PRODUTO_REVENDA';

  return (
    <div className="space-y-6">
      <PageHeader
        title="Saídas"
        description="Movimentação de itens do cardápio ou insumos do estoque. Insumos vendidos pela primeira vez são cadastrados automaticamente no cardápio."
      />

      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicarFiltros();
        }}
      >
        <div className="space-y-1.5">
          <Label htmlFor="busca-venda">Buscar produto ou insumo</Label>
          <div className="relative">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              id="busca-venda"
              value={buscaInput}
              onChange={(e) => setBuscaInput(e.target.value)}
              placeholder="Nome ou código…"
              className="pl-9"
            />
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" onClick={limparFiltros}>
            <X className="h-4 w-4" /> Limpar
          </Button>
          <Button type="submit">
            <Search className="h-4 w-4" /> Pesquisar
          </Button>
        </div>
      </form>

      {!filialId && (
        <Card>
          <CardContent className="py-6 text-center text-sm text-muted-foreground">
            Selecione uma filial no topo da página para registrar vendas.
          </CardContent>
        </Card>
      )}

      {filialId && itensFiltrados.length === 0 && !cardapioQuery.isLoading && (
        <Card>
          <CardContent className="py-6 text-center text-sm text-muted-foreground">
            {busca
              ? `Nada bate com "${busca}".`
              : 'Nenhum item disponível para venda nesta filial.'}
          </CardContent>
        </Card>
      )}

      <ConfirmarVendaDialog
        open={!!confirmacao && usaPreviewDeFicha}
        onOpenChange={(open) => {
          if (!open && !confirmando) setConfirmacao(null);
        }}
        produtoVendavelId={usaPreviewDeFicha ? confirmacao!.id : null}
        produtoNome={confirmacao?.nome ?? ''}
        filialId={filialId ?? ''}
        quantidade={confirmacao?.quantidade ?? 0}
        confirmando={confirmando}
        onConfirm={confirmarVenda}
      />

      <ConfirmarInsumoDialog
        open={!!confirmacao && confirmacao.origem === 'INSUMO_ORFAO'}
        onOpenChange={(open) => {
          if (!open && !confirmando) setConfirmacao(null);
        }}
        nome={confirmacao?.nome ?? ''}
        quantidade={confirmacao?.quantidade ?? 0}
        confirmando={confirmando}
        onConfirm={confirmarVenda}
      />

      {filialId && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {itensFiltrados.map((it) => (
            <ItemCard
              key={`${it.origem}-${it.id}`}
              item={it}
              quantidade={quantidades[it.id] ?? ''}
              onChangeQuantidade={(v) => setQuantidades((prev) => ({ ...prev, [it.id]: v }))}
              onVender={() => handleVender(it)}
              vendendo={confirmando && confirmacao?.id === it.id}
            />
          ))}
        </div>
      )}
    </div>
  );
}

interface ItemCardProps {
  item: ItemCardapio;
  quantidade: string;
  onChangeQuantidade: (v: string) => void;
  onVender: () => void;
  vendendo: boolean;
}

function ItemCard({ item, quantidade, onChangeQuantidade, onVender, vendendo }: ItemCardProps) {
  const badge = badgeForOrigem(item.origem);
  return (
    <Card>
      <CardContent className="p-4 space-y-3">
        <div>
          <div className="flex items-center justify-between gap-2">
            <code className="text-[10px] uppercase tracking-wide text-muted-foreground">
              {item.codigo}
            </code>
            <span className={`text-[10px] font-medium px-1.5 py-0.5 rounded ${badge.classes}`}>
              {badge.label}
            </span>
          </div>
          <h3 className="font-display text-lg leading-tight">{item.nome}</h3>
          {item.categoria && (
            <p className="text-xs text-muted-foreground">{item.categoria}</p>
          )}
          {item.saldoNaFilial !== null && (
            <p className="text-xs text-muted-foreground mt-1">
              Saldo: {formatSaldo(item.saldoNaFilial)} {item.unidadeBaseCodigo}
            </p>
          )}
          {item.vendasUltimos30Dias > 0 && (
            <p className="text-xs text-muted-foreground mt-1">
              {item.vendasUltimos30Dias} venda{item.vendasUltimos30Dias === 1 ? '' : 's'} nos últimos 30d
            </p>
          )}
        </div>
        <div className="flex items-end gap-2">
          <div className="flex-1 space-y-1">
            <Label htmlFor={`qty-${item.id}`} className="text-xs">
              Quantidade
            </Label>
            <Input
              id={`qty-${item.id}`}
              type="number"
              min="0"
              step="1"
              value={quantidade}
              onChange={(e) => onChangeQuantidade(e.target.value)}
              placeholder="1"
            />
          </div>
          <Button onClick={onVender} disabled={vendendo}>
            <Receipt className="h-4 w-4" />
            {vendendo ? 'Vendendo…' : 'Vender'}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function badgeForOrigem(o: OrigemItemCardapio): { label: string; classes: string } {
  switch (o) {
    case 'PRODUTO_FABRICADO':
      return { label: 'Cardápio', classes: 'bg-primary/10 text-primary' };
    case 'PRODUTO_REVENDA':
      return { label: 'Revenda', classes: 'bg-amber-100 text-amber-800' };
    case 'INSUMO_ORFAO':
      return { label: 'Estoque', classes: 'bg-muted text-muted-foreground' };
  }
}

function formatSaldo(n: number): string {
  return n.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 3 });
}

// Dialog mais simples pra venda de insumo órfão — sem preview de ficha técnica.
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

function ConfirmarInsumoDialog({
  open,
  onOpenChange,
  nome,
  quantidade,
  confirmando,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  nome: string;
  quantidade: number;
  confirmando: boolean;
  onConfirm: () => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Confirmar venda</DialogTitle>
          <DialogDescription>
            {nome} × {quantidade}
          </DialogDescription>
        </DialogHeader>
        <div className="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
          Este insumo ainda não está no cardápio. Ao confirmar, ele será cadastrado automaticamente
          (categoria "A classificar") e a venda registrada. Você pode renomear/categorizar depois em
          Cardápio.
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={confirmando}>
            Cancelar
          </Button>
          <Button onClick={onConfirm} disabled={confirmando}>
            <Receipt className="h-4 w-4" />
            {confirmando ? 'Registrando…' : 'Cadastrar e vender'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
