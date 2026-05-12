import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Power, Search, X } from 'lucide-react';
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
  type Empresa,
  ativarEmpresa,
  desativarEmpresa,
  listarEmpresas,
} from './api';
import { EmpresaFormDialog } from './EmpresaFormDialog';

const ATIVA_TODOS = '__todos__';

export function EmpresasPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Empresa | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Inputs (não disparam filtragem).
  const [buscaInput, setBuscaInput] = useState('');
  const [ativaInput, setAtivaInput] = useState(ATIVA_TODOS);

  // Filtros aplicados (só mudam ao clicar Pesquisar).
  const [filtros, setFiltros] = useState({ q: '', ativa: ATIVA_TODOS });

  const empresasQuery = useQuery({
    queryKey: ['admin-empresas'],
    queryFn: listarEmpresas,
  });

  const filtradas = useMemo(() => {
    const todas = empresasQuery.data ?? [];
    const termo = filtros.q.trim().toLowerCase();
    return todas.filter((e) => {
      if (filtros.ativa === 'true' && !e.ativa) return false;
      if (filtros.ativa === 'false' && e.ativa) return false;
      if (
        termo &&
        !e.razaoSocial.toLowerCase().includes(termo) &&
        !e.cnpj.includes(termo)
      ) {
        return false;
      }
      return true;
    });
  }, [empresasQuery.data, filtros]);

  function aplicarFiltros() {
    setFiltros({ q: buscaInput, ativa: ativaInput });
  }

  function limparFiltros() {
    setBuscaInput('');
    setAtivaInput(ATIVA_TODOS);
    setFiltros({ q: '', ativa: ATIVA_TODOS });
  }

  const desativarMutation = useMutation({
    mutationFn: desativarEmpresa,
    onSuccess: () => {
      toast.success('Empresa desativada');
      queryClient.invalidateQueries({ queryKey: ['admin-empresas'] });
      queryClient.invalidateQueries({ queryKey: ['empresas'] });
    },
    onError: (error) => toastError('Não foi possível desativar', error),
  });
  const ativarMutation = useMutation({
    mutationFn: ativarEmpresa,
    onSuccess: () => {
      toast.success('Empresa reativada');
      queryClient.invalidateQueries({ queryKey: ['admin-empresas'] });
      queryClient.invalidateQueries({ queryKey: ['empresas'] });
    },
    onError: (error) => toastError('Não foi possível ativar', error),
  });
  const togglingId = desativarMutation.isPending
    ? desativarMutation.variables
    : ativarMutation.isPending
      ? ativarMutation.variables
      : undefined;

  const columns: ColumnDef<Empresa>[] = [
    {
      key: 'razaoSocial',
      header: 'Razão social',
      cell: (e) => <span className="font-medium">{e.razaoSocial}</span>,
    },
    { key: 'cnpj', header: 'CNPJ', cell: (e) => e.cnpjFormatado },
    { key: 'status', header: 'Status', cell: (e) => <StatusBadge active={e.ativa} /> },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[200px]',
      cell: (e) => (
        <div className="flex justify-end gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setEditing(e);
              setDialogOpen(true);
            }}
          >
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === e.id}
            onClick={() =>
              e.ativa ? desativarMutation.mutate(e.id) : ativarMutation.mutate(e.id)
            }
          >
            <Power className="h-4 w-4" /> {e.ativa ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Empresas"
        description="Cadastro das empresas do grupo Nonnas Paola — referenciadas pelas filiais."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setDialogOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Nova empresa
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
            <Label htmlFor="filtro-ativa">Status</Label>
            <Select value={ativaInput} onValueChange={setAtivaInput}>
              <SelectTrigger id="filtro-ativa">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ATIVA_TODOS}>Todas</SelectItem>
                <SelectItem value="true">Ativas</SelectItem>
                <SelectItem value="false">Inativas</SelectItem>
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
        data={filtradas}
        columns={columns}
        isLoading={empresasQuery.isLoading}
        isError={empresasQuery.isError}
        rowKey={(e) => e.id}
        rowClassName={(e) => (!e.ativa ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhuma empresa cadastrada ainda.</p>
            <Button
              variant="outline"
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4" /> Cadastrar empresa
            </Button>
          </div>
        }
      />

      <EmpresaFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        empresa={editing}
      />
    </div>
  );
}
