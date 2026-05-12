import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link2, Pencil, Plus, Power, Search, X } from 'lucide-react';
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
import { FornecedorDeParaDialog } from './FornecedorDeParaDialog';

const ATIVO_TODOS = '__todos__';

export function FornecedoresPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Fornecedor | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deparaFornecedor, setDeparaFornecedor] = useState<Fornecedor | null>(null);

  // Inputs (não disparam query).
  const [buscaInput, setBuscaInput] = useState('');
  const [ativoInput, setAtivoInput] = useState(ATIVO_TODOS);

  // Filtros aplicados (só mudam ao clicar Pesquisar).
  const [filtrosAplicados, setFiltrosAplicados] = useState({
    q: '',
    ativo: ATIVO_TODOS,
  });

  const filtros = useMemo(
    () => ({
      ativo: filtrosAplicados.ativo === ATIVO_TODOS ? undefined : filtrosAplicados.ativo === 'true',
      q: filtrosAplicados.q.trim() || undefined,
    }),
    [filtrosAplicados],
  );

  function aplicarFiltros() {
    setFiltrosAplicados({ q: buscaInput, ativo: ativoInput });
  }

  function limparFiltros() {
    setBuscaInput('');
    setAtivoInput(ATIVO_TODOS);
    setFiltrosAplicados({ q: '', ativo: ATIVO_TODOS });
  }

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
  const togglingId = desativarMutation.isPending
    ? desativarMutation.variables
    : ativarMutation.isPending
      ? ativarMutation.variables
      : undefined;

  const columns: ColumnDef<Fornecedor>[] = [
    { key: 'razaoSocial', header: 'Razão social', cell: (f) => <span className="font-medium">{f.razaoSocial}</span> },
    { key: 'cnpj', header: 'CNPJ', cell: (f) => f.cnpjFormatado },
    { key: 'status', header: 'Status', cell: (f) => <StatusBadge active={f.ativo} /> },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[320px]',
      cell: (f) => (
        <div className="flex justify-end gap-2">
          <Button variant="ghost" size="sm" onClick={() => setDeparaFornecedor(f)}>
            <Link2 className="h-4 w-4" /> Mapeamentos
          </Button>
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

      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicarFiltros();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5 md:col-span-2">
            <Label htmlFor="filtro-busca">Buscar</Label>
            <Input
              id="filtro-busca"
              placeholder="Razão social ou CNPJ"
              value={buscaInput}
              onChange={(e) => setBuscaInput(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-ativo">Status</Label>
            <Select value={ativoInput} onValueChange={setAtivoInput}>
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
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" onClick={limparFiltros}>
            <X className="h-4 w-4" /> Limpar
          </Button>
          <Button type="submit">
            <Search className="h-4 w-4" /> Pesquisar
          </Button>
        </div>
      </form>

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

      <FornecedorDeParaDialog
        open={!!deparaFornecedor}
        onOpenChange={(o) => { if (!o) setDeparaFornecedor(null); }}
        fornecedorId={deparaFornecedor?.id ?? null}
        fornecedorNome={deparaFornecedor?.razaoSocial ?? ''}
      />
    </div>
  );
}
