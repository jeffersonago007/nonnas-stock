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
  type Perfil,
  type Usuario,
  ativarUsuario,
  desativarUsuario,
  listarUsuarios,
} from './api';
import { UsuarioFormDialog } from './UsuarioFormDialog';

const ATIVO_TODOS = '__todos__';
const PERFIL_TODOS = '__todos__';

const perfilLabel: Record<Perfil, string> = {
  ADMIN: 'Administrador',
  GERENTE: 'Gerente',
  OPERADOR: 'Operador',
  CONSULTA: 'Consulta',
};

export function UsuariosPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Usuario | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [busca, setBusca] = useState('');
  const [ativoFiltro, setAtivoFiltro] = useState(ATIVO_TODOS);
  const [perfilFiltro, setPerfilFiltro] = useState(PERFIL_TODOS);

  const usuariosQuery = useQuery({
    queryKey: ['admin-usuarios'],
    queryFn: listarUsuarios,
  });

  const filtrados = useMemo(() => {
    const todos = usuariosQuery.data ?? [];
    const termo = busca.trim().toLowerCase();
    return todos.filter((u) => {
      if (ativoFiltro === 'true' && !u.ativo) return false;
      if (ativoFiltro === 'false' && u.ativo) return false;
      if (perfilFiltro !== PERFIL_TODOS && u.perfil !== perfilFiltro) return false;
      if (
        termo &&
        !u.nome.toLowerCase().includes(termo) &&
        !u.email.toLowerCase().includes(termo)
      ) {
        return false;
      }
      return true;
    });
  }, [usuariosQuery.data, busca, ativoFiltro, perfilFiltro]);

  const desativarMutation = useMutation({
    mutationFn: desativarUsuario,
    onSuccess: () => {
      toast.success('Usuário desativado');
      queryClient.invalidateQueries({ queryKey: ['admin-usuarios'] });
    },
    onError: (error) => toastError('Não foi possível desativar', error),
  });
  const ativarMutation = useMutation({
    mutationFn: ativarUsuario,
    onSuccess: () => {
      toast.success('Usuário reativado');
      queryClient.invalidateQueries({ queryKey: ['admin-usuarios'] });
    },
    onError: (error) => toastError('Não foi possível ativar', error),
  });
  const togglingId = desativarMutation.variables ?? ativarMutation.variables;

  const columns: ColumnDef<Usuario>[] = [
    {
      key: 'nome',
      header: 'Nome',
      cell: (u) => (
        <div className="flex flex-col">
          <span className="font-medium">{u.nome}</span>
          <span className="text-xs text-muted-foreground">{u.email}</span>
        </div>
      ),
    },
    {
      key: 'perfil',
      header: 'Perfil',
      cell: (u) => perfilLabel[u.perfil],
      className: 'w-[150px]',
    },
    {
      key: 'status',
      header: 'Status',
      cell: (u) => (
        <div className="flex flex-col gap-1">
          <StatusBadge active={u.ativo} />
          {u.travada && (
            <span className="text-xs text-destructive">Conta travada</span>
          )}
        </div>
      ),
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[200px]',
      cell: (u) => (
        <div className="flex justify-end gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setEditing(u);
              setDialogOpen(true);
            }}
          >
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === u.id}
            onClick={() =>
              u.ativo ? desativarMutation.mutate(u.id) : ativarMutation.mutate(u.id)
            }
          >
            <Power className="h-4 w-4" /> {u.ativo ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Usuários"
        description="Gestão dos usuários do sistema e seus perfis de acesso."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setDialogOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Novo usuário
          </Button>
        }
      />

      <div className="grid gap-3 rounded-md border bg-card p-4 md:grid-cols-4">
        <div className="space-y-1.5 md:col-span-2">
          <Label htmlFor="filtro-busca">Buscar</Label>
          <Input
            id="filtro-busca"
            placeholder="Nome ou e-mail"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filtro-perfil">Perfil</Label>
          <Select value={perfilFiltro} onValueChange={setPerfilFiltro}>
            <SelectTrigger id="filtro-perfil">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={PERFIL_TODOS}>Todos</SelectItem>
              <SelectItem value="ADMIN">Administrador</SelectItem>
              <SelectItem value="GERENTE">Gerente</SelectItem>
              <SelectItem value="OPERADOR">Operador</SelectItem>
              <SelectItem value="CONSULTA">Consulta</SelectItem>
            </SelectContent>
          </Select>
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
        data={filtrados}
        columns={columns}
        isLoading={usuariosQuery.isLoading}
        isError={usuariosQuery.isError}
        rowKey={(u) => u.id}
        rowClassName={(u) => (!u.ativo ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhum usuário encontrado.</p>
            <Button
              variant="outline"
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4" /> Cadastrar usuário
            </Button>
          </div>
        }
      />

      <UsuarioFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        usuario={editing}
      />
    </div>
  );
}
