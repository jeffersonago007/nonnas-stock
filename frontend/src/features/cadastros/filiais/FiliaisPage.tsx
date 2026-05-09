import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Pencil, Plus, Power, Upload } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { PageHeader } from '@/components/PageHeader';
import { StatusBadge } from '@/components/StatusBadge';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';

import {
  type Filial,
  ativarFilial,
  desativarFilial,
  listarFiliais,
} from './api';
import { FilialFormDialog } from './FilialFormDialog';

export function FiliaisPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Filial | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  const filiaisQuery = useQuery({
    queryKey: ['filiais'],
    queryFn: () => listarFiliais(),
  });

  const desativarMutation = useMutation({
    mutationFn: desativarFilial,
    onSuccess: () => {
      toast.success('Filial desativada');
      queryClient.invalidateQueries({ queryKey: ['filiais'] });
    },
    onError: (error) => toastError('Não foi possível desativar', error),
  });

  const ativarMutation = useMutation({
    mutationFn: ativarFilial,
    onSuccess: () => {
      toast.success('Filial reativada');
      queryClient.invalidateQueries({ queryKey: ['filiais'] });
    },
    onError: (error) => toastError('Não foi possível ativar', error),
  });

  const togglingId = desativarMutation.variables ?? ativarMutation.variables;

  const columns: ColumnDef<Filial>[] = [
    { key: 'nome', header: 'Nome', cell: (f) => <span className="font-medium">{f.nome}</span> },
    { key: 'cnpj', header: 'CNPJ', cell: (f) => f.cnpjFormatado },
    {
      key: 'endereco',
      header: 'Endereço',
      cell: (f) => f.endereco ?? <span className="text-muted-foreground">—</span>,
    },
    {
      key: 'ativa',
      header: 'Status',
      cell: (f) => <StatusBadge active={f.ativa} activeLabel="Ativa" inactiveLabel="Inativa" />,
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[280px]',
      cell: (f) => (
        <div className="flex justify-end gap-2">
          <Button asChild variant="ghost" size="sm">
            <Link to={`/filiais/${f.id}/carga-inicial`}>
              <Upload className="h-4 w-4" /> Carga inicial
            </Link>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setEditing(f);
              setDialogOpen(true);
            }}
          >
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === f.id}
            onClick={() => (f.ativa ? desativarMutation.mutate(f.id) : ativarMutation.mutate(f.id))}
          >
            <Power className="h-4 w-4" /> {f.ativa ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Filiais"
        description="Cadastro das filiais da rede e ponto de entrada para carga inicial de estoque."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setDialogOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Nova filial
          </Button>
        }
      />

      <DataTable
        data={filiaisQuery.data}
        columns={columns}
        isLoading={filiaisQuery.isLoading}
        isError={filiaisQuery.isError}
        rowKey={(f) => f.id}
        rowClassName={(f) => (!f.ativa ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhuma filial cadastrada ainda.</p>
            <Button
              variant="outline"
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4" /> Cadastrar primeira filial
            </Button>
          </div>
        }
      />

      <FilialFormDialog open={dialogOpen} onOpenChange={setDialogOpen} filial={editing} />
    </div>
  );
}
