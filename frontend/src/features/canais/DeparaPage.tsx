import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';
import { listarFiliais } from '@/features/cadastros/filiais/api';
import { listarProdutos } from '@/features/cadastros/produtos/api';

import {
  type CanalTipo,
  type CanalProdutoDePara,
  CANAL_LABEL,
  CANAIS,
  deletarDepara,
  listarDepara,
} from './api';
import { DeparaFormDialog } from './DeparaFormDialog';

export function DeparaPage() {
  const queryClient = useQueryClient();
  const [canal, setCanal] = useState<CanalTipo>('IFOOD');
  const [editing, setEditing] = useState<CanalProdutoDePara | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  const deparaQuery = useQuery({
    queryKey: ['canais-depara', canal],
    queryFn: () => listarDepara(canal),
  });
  const produtosQuery = useQuery({
    queryKey: ['produtos-vendaveis-ativos'],
    queryFn: () => listarProdutos({ ativo: true }),
  });
  const filiaisQuery = useQuery({
    queryKey: ['filiais'],
    queryFn: () => listarFiliais(),
  });

  const produtoMap = useMemo(() => {
    const m = new Map<string, string>();
    produtosQuery.data?.forEach((p) => m.set(p.id, p.nome));
    return m;
  }, [produtosQuery.data]);

  const filialMap = useMemo(() => {
    const m = new Map<string, string>();
    filiaisQuery.data?.forEach((f) => m.set(f.id, f.nome));
    return m;
  }, [filiaisQuery.data]);

  const ordenados = useMemo(() => {
    const base = deparaQuery.data ?? [];
    return [...base].sort((a, b) => {
      const ec = a.externalCode.localeCompare(b.externalCode, 'pt-BR', { sensitivity: 'base' });
      if (ec !== 0) return ec;
      // Global antes de específico-filial (mesmo externalCode).
      if ((a.filialId === null) !== (b.filialId === null)) {
        return a.filialId === null ? -1 : 1;
      }
      const fa = a.filialId ? filialMap.get(a.filialId) ?? '' : '';
      const fb = b.filialId ? filialMap.get(b.filialId) ?? '' : '';
      return fa.localeCompare(fb, 'pt-BR', { sensitivity: 'base' });
    });
  }, [deparaQuery.data, filialMap]);

  const deletarMut = useMutation({
    mutationFn: deletarDepara,
    onSuccess: () => {
      toast.success('Mapeamento removido');
      queryClient.invalidateQueries({ queryKey: ['canais-depara'] });
    },
    onError: (e) => toastError('Não foi possível remover', e),
  });

  const columns: ColumnDef<CanalProdutoDePara>[] = [
    {
      key: 'externalCode',
      header: 'Código externo',
      cell: (d) => <code className="text-xs">{d.externalCode}</code>,
      className: 'w-[200px]',
    },
    {
      key: 'produto',
      header: 'Produto vendável',
      cell: (d) => produtoMap.get(d.produtoVendavelId) ?? d.produtoVendavelId,
    },
    {
      key: 'escopo',
      header: 'Escopo',
      cell: (d) =>
        d.filialId === null ? (
          <span className="rounded bg-muted px-2 py-0.5 text-xs font-medium">Global</span>
        ) : (
          <span className="text-xs">{filialMap.get(d.filialId) ?? 'Filial'}</span>
        ),
      className: 'w-[180px]',
    },
    {
      key: 'observacao',
      header: 'Observação',
      cell: (d) =>
        d.observacao ? (
          <span className="text-xs">{d.observacao}</span>
        ) : (
          <span className="text-muted-foreground text-xs">—</span>
        ),
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[180px]',
      cell: (d) => (
        <div className="flex justify-end gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setEditing(d);
              setDialogOpen(true);
            }}
          >
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={deletarMut.isPending && deletarMut.variables === d.id}
            onClick={() => {
              if (confirm(`Remover mapeamento "${d.externalCode}"?`)) {
                deletarMut.mutate(d.id);
              }
            }}
          >
            <Trash2 className="h-4 w-4" /> Remover
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="De-para de produtos"
        description="Mapeia o código externo de cada item do canal para um produto vendável do cardápio. Mapeamento global vale para todas as filiais; específico de filial tem precedência."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setDialogOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Novo mapeamento
          </Button>
        }
      />

      <div className="rounded-md border bg-card p-4">
        <div className="grid gap-3 md:grid-cols-4">
          <div className="space-y-1.5">
            <Label htmlFor="canal">Canal</Label>
            <Select value={canal} onValueChange={(v) => setCanal(v as CanalTipo)}>
              <SelectTrigger id="canal">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CANAIS.map((c) => (
                  <SelectItem key={c} value={c}>
                    {CANAL_LABEL[c]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      <DataTable
        data={ordenados}
        columns={columns}
        isLoading={deparaQuery.isLoading}
        isError={deparaQuery.isError}
        rowKey={(d) => d.id}
        emptyState={
          <div className="space-y-3">
            <p>Nenhum mapeamento cadastrado para {CANAL_LABEL[canal]}.</p>
            <Button
              variant="outline"
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4" /> Cadastrar mapeamento
            </Button>
          </div>
        }
      />

      <DeparaFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        depara={editing}
        canalAtual={canal}
        existentesNoCanal={deparaQuery.data ?? []}
        produtos={produtosQuery.data ?? []}
        filiais={filiaisQuery.data ?? []}
      />
    </div>
  );
}
