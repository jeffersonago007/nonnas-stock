import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Power } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
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

import {
  type Fornecedor,
  ativarFornecedor,
  desativarFornecedor,
  listarFornecedores,
} from './api';
import { FornecedorFormDialog } from './FornecedorFormDialog';

const ATIVO_TODOS = '__todos__';

export function FornecedoresPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Fornecedor | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [busca, setBusca] = useState('');
  const [ativoFiltro, setAtivoFiltro] = useState(ATIVO_TODOS);

  const filtros = useMemo(
    () => ({
      ativo: ativoFiltro === ATIVO_TODOS ? undefined : ativoFiltro === 'true',
      q: busca.trim() || undefined,
    }),
    [ativoFiltro, busca],
  );

  const fornecedoresQuery = useQuery({
    queryKey: ['fornecedores', filtros],
    queryFn: () => listarFornecedores(filtros),
  });

  const desativarMutation = useMutation({
    mutationFn: desativarFornecedor,
    onSuccess: () => {
      toast.success('Fornecedor desativado');
      queryClient.invalidateQueries({ queryKey: ['fornecedores'] });
    },
    onError: (error) => toastError('Não foi possível desativar', error),
  });
  const ativarMutation = useMutation({
    mutationFn: ativarFornecedor,
    onSuccess: () => {
      toast.success('Fornecedor reativado');
      queryClient.invalidateQueries({ queryKey: ['fornecedores'] });
    },
    onError: (error) => toastError('Não foi possível ativar', error),
  });
  const togglingId = desativarMutation.variables ?? ativarMutation.variables;

  const columns: ColumnDef<Fornecedor>[] = [
    { key: 'razaoSocial', header: 'Razão social', cell: (f) => <span className="font-medium">{f.razaoSocial}</span> },
    { key: 'cnpj', header: 'CNPJ', cell: (f) => f.cnpjFormatado },
    { key: 'status', header: 'Status', cell: (f) => <StatusBadge active={f.ativo} /> },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[200px]',
      cell: (f) => (
        <div className="flex justify-end gap-2">
          <Button variant="ghost" size="sm" onClick={() => { setEditing(f); setDialogOpen(true); }}>
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === f.id}
            onClick={() => (f.ativo ? desativarMutation.mutate(f.id) : ativarMutation.mutate(f.id))}
          >
            <Power className="h-4 w-4" /> {f.ativo ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Fornecedores"
        description="Cadastro de fornecedores via CNPJ."
        actions={
          <Button onClick={() => { setEditing(null); setDialogOpen(true); }}>
            <Plus className="h-4 w-4" /> Novo fornecedor
          </Button>
        }
      />

      <div className="grid gap-3 rounded-md border bg-card p-4 md:grid-cols-3">
        <div className="space-y-1.5 md:col-span-2">
          <Label htmlFor="filtro-busca">Buscar</Label>
          <Input
            id="filtro-busca"
            placeholder="Razão social ou CNPJ"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filtro-ativo">Status</Label>
          <Select value={ativoFiltro} onValueChange={setAtivoFiltro}>
            <SelectTrigger id="filtro-ativo">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ATIVO_TODOS}>Todos</SelectItem>
              <SelectItem value="true">Ativos</SelectItem>
              <SelectItem value="false">Inativos</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <DataTable
        data={fornecedoresQuery.data}
        columns={columns}
        isLoading={fornecedoresQuery.isLoading}
        isError={fornecedoresQuery.isError}
        rowKey={(f) => f.id}
        rowClassName={(f) => (!f.ativo ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhum fornecedor cadastrado ainda.</p>
            <Button variant="outline" onClick={() => { setEditing(null); setDialogOpen(true); }}>
              <Plus className="h-4 w-4" /> Cadastrar fornecedor
            </Button>
          </div>
        }
      />

      <FornecedorFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        fornecedor={editing}
      />
    </div>
  );
}
