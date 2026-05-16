import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Power } from 'lucide-react';
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
import { StatusBadge } from '@/components/StatusBadge';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';
import { listarFiliais } from '@/features/cadastros/filiais/api';

import {
  type CanalTipo,
  type CredencialCanal,
  CANAL_LABEL,
  CANAIS,
  ativarCredencial,
  desativarCredencial,
  listarCredenciais,
} from './api';
import { CredencialFormDialog } from './CredencialFormDialog';

const CANAL_TODOS = '__todos__';

export function CredenciaisPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<CredencialCanal | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [canalFiltro, setCanalFiltro] = useState<string>(CANAL_TODOS);

  const credenciaisQuery = useQuery({
    queryKey: ['canais-credenciais', canalFiltro],
    queryFn: () => listarCredenciais(canalFiltro !== CANAL_TODOS ? (canalFiltro as CanalTipo) : undefined),
  });

  const filiaisQuery = useQuery({
    queryKey: ['filiais'],
    queryFn: () => listarFiliais(),
  });

  const filialMap = useMemo(() => {
    const m = new Map<string, string>();
    filiaisQuery.data?.forEach((f) => m.set(f.id, f.nome));
    return m;
  }, [filiaisQuery.data]);

  const credenciaisOrdenadas = useMemo(() => {
    const base = credenciaisQuery.data ?? [];
    return [...base].sort((a, b) => {
      if (a.ativa !== b.ativa) return a.ativa ? -1 : 1;
      if (a.canalTipo !== b.canalTipo) return a.canalTipo.localeCompare(b.canalTipo);
      const fa = filialMap.get(a.filialId) ?? '';
      const fb = filialMap.get(b.filialId) ?? '';
      return fa.localeCompare(fb, 'pt-BR', { sensitivity: 'base' });
    });
  }, [credenciaisQuery.data, filialMap]);

  const desativarMut = useMutation({
    mutationFn: desativarCredencial,
    onSuccess: () => {
      toast.success('Credencial desativada');
      queryClient.invalidateQueries({ queryKey: ['canais-credenciais'] });
    },
    onError: (e) => toastError('Não foi possível desativar', e),
  });
  const ativarMut = useMutation({
    mutationFn: ativarCredencial,
    onSuccess: () => {
      toast.success('Credencial ativada');
      queryClient.invalidateQueries({ queryKey: ['canais-credenciais'] });
    },
    onError: (e) => toastError('Não foi possível ativar', e),
  });
  const togglingId = desativarMut.isPending
    ? desativarMut.variables
    : ativarMut.isPending
      ? ativarMut.variables
      : undefined;

  const columns: ColumnDef<CredencialCanal>[] = [
    {
      key: 'canal',
      header: 'Canal',
      cell: (c) => (
        <div className="flex flex-col">
          <span className="font-medium">{CANAL_LABEL[c.canalTipo]}</span>
          <span className="text-xs text-muted-foreground">{c.merchantExternoId}</span>
        </div>
      ),
      className: 'w-[200px]',
    },
    {
      key: 'filial',
      header: 'Filial',
      cell: (c) => filialMap.get(c.filialId) ?? <span className="text-muted-foreground">—</span>,
      className: 'w-[200px]',
    },
    {
      key: 'clientId',
      header: 'Client ID',
      cell: (c) => <code className="text-xs">{c.clientId}</code>,
    },
    {
      key: 'baseUrl',
      header: 'Base URL',
      cell: (c) =>
        c.baseUrl ? (
          <code className="text-xs">{c.baseUrl}</code>
        ) : (
          <span className="text-muted-foreground text-xs">padrão do canal</span>
        ),
    },
    {
      key: 'status',
      header: 'Status',
      cell: (c) => <StatusBadge active={c.ativa} />,
      className: 'w-[120px]',
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[200px]',
      cell: (c) => (
        <div className="flex justify-end gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setEditing(c);
              setDialogOpen(true);
            }}
          >
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === c.id}
            onClick={() => (c.ativa ? desativarMut.mutate(c.id) : ativarMut.mutate(c.id))}
          >
            <Power className="h-4 w-4" /> {c.ativa ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Credenciais dos canais"
        description="Cadastre as credenciais OAuth/API dos canais (iFood, 99Food, Keeta) por filial. Segredo é cifrado antes de salvar; nunca volta no response."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setDialogOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Nova credencial
          </Button>
        }
      />

      <div className="rounded-md border bg-card p-4">
        <div className="grid gap-3 md:grid-cols-4">
          <div className="space-y-1.5">
            <Label htmlFor="filtro-canal">Canal</Label>
            <Select value={canalFiltro} onValueChange={setCanalFiltro}>
              <SelectTrigger id="filtro-canal">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={CANAL_TODOS}>Todos</SelectItem>
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
        data={credenciaisOrdenadas}
        columns={columns}
        isLoading={credenciaisQuery.isLoading}
        isError={credenciaisQuery.isError}
        rowKey={(c) => c.id}
        rowClassName={(c) => (!c.ativa ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhuma credencial cadastrada.</p>
            <Button
              variant="outline"
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4" /> Cadastrar credencial
            </Button>
          </div>
        }
      />

      <CredencialFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        credencial={editing}
      />
    </div>
  );
}
