import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell, BellOff, CheckCircle2, Pencil, Plus } from 'lucide-react';
import { toast } from 'sonner';
import { z } from 'zod';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';

import { listarFiliais } from '@/features/cadastros/filiais/api';
import { listarInsumos } from '@/features/cadastros/insumos/api';
import { useAuthStore } from '@/features/auth/store';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';

import {
  type AlertaConfig,
  type AlertaDisparado,
  type Prioridade,
  type StatusAlerta,
  type TipoAlerta,
  atualizarConfig,
  criarConfig,
  listarConfigs,
  listarDisparados,
  resolverDisparado,
} from './api';

const SCOPE_REDE = '__rede__';

const TIPO_LABELS: Record<TipoAlerta, string> = {
  ESTOQUE_MINIMO_PERCENTUAL: 'Estoque mínimo (%)',
  ESTOQUE_MINIMO_ABSOLUTO: 'Estoque mínimo (absoluto)',
  VENCIMENTO_PROXIMO_DIAS: 'Vencimento próximo (dias)',
  RUPTURA: 'Ruptura',
};

const PRIORIDADE_LABELS: Record<Prioridade, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
};

const STATUS_LABELS: Record<StatusAlerta, string> = {
  ATIVO: 'Ativo',
  RESOLVIDO_AUTO: 'Resolvido (auto)',
  RESOLVIDO_MANUAL: 'Resolvido (manual)',
};

const STATUS_TONES: Record<StatusAlerta, string> = {
  ATIVO: 'bg-destructive/10 text-destructive',
  RESOLVIDO_AUTO: 'bg-secondary/10 text-secondary',
  RESOLVIDO_MANUAL: 'bg-muted text-muted-foreground',
};

export function AlertasPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Alertas"
        description="Configure thresholds e acompanhe disparos. Ruptura, estoque mínimo (% ou absoluto) e vencimento próximo."
      />
      <Tabs defaultValue="disparados" className="space-y-4">
        <TabsList>
          <TabsTrigger value="disparados">
            <Bell className="mr-2 h-4 w-4" /> Disparados
          </TabsTrigger>
          <TabsTrigger value="configs">
            <BellOff className="mr-2 h-4 w-4" /> Configurações
          </TabsTrigger>
        </TabsList>
        <TabsContent value="disparados">
          <DisparadosTab />
        </TabsContent>
        <TabsContent value="configs">
          <ConfigsTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ─────────────────────────── Tab Disparados ──────────────────────────────

function DisparadosTab() {
  const queryClient = useQueryClient();
  const usuarioId = useAuthStore((s) => s.user?.id);
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const [statusFiltro, setStatusFiltro] = useState<StatusAlerta | '__todos__'>('ATIVO');
  const [tipoFiltro, setTipoFiltro] = useState<TipoAlerta | '__todos__'>('__todos__');

  const filtros = useMemo(
    () => ({
      filialId,
      status: statusFiltro === '__todos__' ? null : statusFiltro,
      tipo: tipoFiltro === '__todos__' ? null : tipoFiltro,
    }),
    [filialId, statusFiltro, tipoFiltro],
  );

  const disparadosQuery = useQuery({
    queryKey: ['alertas-disparados', filtros],
    queryFn: () => listarDisparados(filtros),
  });

  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const filiaisQuery = useQuery({ queryKey: ['filiais'], queryFn: () => listarFiliais() });
  const insumoMap = useMemo(() => {
    const m = new Map<string, string>();
    insumosQuery.data?.forEach((i) => m.set(i.id, i.nome));
    return m;
  }, [insumosQuery.data]);
  const filialMap = useMemo(() => {
    const m = new Map<string, string>();
    filiaisQuery.data?.forEach((f) => m.set(f.id, f.nome));
    return m;
  }, [filiaisQuery.data]);

  const resolverMut = useMutation({
    mutationFn: (id: string) => resolverDisparado(id, usuarioId ?? ''),
    onSuccess: () => {
      toast.success('Alerta resolvido');
      queryClient.invalidateQueries({ queryKey: ['alertas-disparados'] });
    },
    onError: (e) => toastError('Não foi possível resolver', e),
  });

  const columns: ColumnDef<AlertaDisparado>[] = [
    {
      key: 'data',
      header: 'Disparado em',
      cell: (a) => (
        <span className="text-sm text-muted-foreground">
          {new Date(a.dataDisparo).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
        </span>
      ),
      className: 'w-[160px]',
    },
    { key: 'tipo', header: 'Tipo', cell: (a) => TIPO_LABELS[a.tipo] },
    {
      key: 'insumo',
      header: 'Insumo',
      cell: (a) => (
        <span className="font-medium">{insumoMap.get(a.insumoId) ?? a.insumoId.slice(0, 8)}</span>
      ),
    },
    {
      key: 'filial',
      header: 'Filial',
      cell: (a) => filialMap.get(a.filialId) ?? a.filialId.slice(0, 8),
    },
    {
      key: 'saldo',
      header: 'Saldo no disparo',
      cell: (a) =>
        a.saldoNoDisparo !== null && a.saldoNoDisparo !== undefined
          ? a.saldoNoDisparo.toLocaleString('pt-BR')
          : '—',
      className: 'text-right',
    },
    {
      key: 'status',
      header: 'Status',
      cell: (a) => (
        <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_TONES[a.status]}`}>
          {STATUS_LABELS[a.status]}
        </span>
      ),
      className: 'w-[160px]',
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[140px]',
      cell: (a) =>
        a.status === 'ATIVO' && (
          <Button
            size="sm"
            variant="ghost"
            disabled={resolverMut.variables === a.id}
            onClick={() => resolverMut.mutate(a.id)}
          >
            <CheckCircle2 className="h-4 w-4" /> Resolver
          </Button>
        ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="grid gap-3 rounded-md border bg-card p-4 md:grid-cols-3">
        <div className="space-y-1.5">
          <Label htmlFor="filtro-status">Status</Label>
          <Select value={statusFiltro} onValueChange={(v) => setStatusFiltro(v as StatusAlerta | '__todos__')}>
            <SelectTrigger id="filtro-status">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__todos__">Todos</SelectItem>
              <SelectItem value="ATIVO">Ativos</SelectItem>
              <SelectItem value="RESOLVIDO_AUTO">Resolvidos (auto)</SelectItem>
              <SelectItem value="RESOLVIDO_MANUAL">Resolvidos (manual)</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filtro-tipo">Tipo</Label>
          <Select value={tipoFiltro} onValueChange={(v) => setTipoFiltro(v as TipoAlerta | '__todos__')}>
            <SelectTrigger id="filtro-tipo">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__todos__">Todos</SelectItem>
              {(Object.keys(TIPO_LABELS) as TipoAlerta[]).map((t) => (
                <SelectItem key={t} value={t}>
                  {TIPO_LABELS[t]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex items-end">
          <p className="text-xs text-muted-foreground">
            Filtro de filial vem do header. Disparos novos aparecem no próximo refetch (30s).
          </p>
        </div>
      </div>

      <DataTable
        data={disparadosQuery.data}
        columns={columns}
        isLoading={disparadosQuery.isLoading}
        isError={disparadosQuery.isError}
        rowKey={(a) => a.id}
        emptyState={<p>Nenhum alerta encontrado para os filtros atuais.</p>}
      />
    </div>
  );
}

// ─────────────────────────── Tab Configurações ───────────────────────────

function ConfigsTab() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<AlertaConfig | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  const configsQuery = useQuery({ queryKey: ['alertas-config'], queryFn: listarConfigs });
  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const filiaisQuery = useQuery({ queryKey: ['filiais'], queryFn: () => listarFiliais() });

  const insumoMap = useMemo(() => {
    const m = new Map<string, string>();
    insumosQuery.data?.forEach((i) => m.set(i.id, i.nome));
    return m;
  }, [insumosQuery.data]);
  const filialMap = useMemo(() => {
    const m = new Map<string, string>();
    filiaisQuery.data?.forEach((f) => m.set(f.id, f.nome));
    return m;
  }, [filiaisQuery.data]);

  const togglarMut = useMutation({
    mutationFn: (config: AlertaConfig) =>
      atualizarConfig(config.id, {
        threshold: config.threshold ?? null,
        prioridade: config.prioridade,
        observacao: config.observacao ?? null,
        ativo: !config.ativo,
      }),
    onSuccess: () => {
      toast.success('Configuração atualizada');
      queryClient.invalidateQueries({ queryKey: ['alertas-config'] });
    },
    onError: (e) => toastError('Não foi possível atualizar', e),
  });

  const columns: ColumnDef<AlertaConfig>[] = [
    { key: 'tipo', header: 'Tipo', cell: (c) => TIPO_LABELS[c.tipo] },
    {
      key: 'escopo',
      header: 'Escopo',
      cell: (c) => {
        const insumo = c.insumoId ? insumoMap.get(c.insumoId) ?? 'Insumo específico' : 'Todos os insumos';
        const filial = c.filialId ? filialMap.get(c.filialId) ?? 'Filial específica' : 'Toda a rede';
        return (
          <div className="text-xs text-muted-foreground">
            <div>{insumo}</div>
            <div>{filial}</div>
          </div>
        );
      },
    },
    {
      key: 'threshold',
      header: 'Threshold',
      cell: (c) =>
        c.threshold !== null && c.threshold !== undefined
          ? c.threshold.toString()
          : <span className="text-muted-foreground">—</span>,
      className: 'text-right w-[120px]',
    },
    {
      key: 'prioridade',
      header: 'Prioridade',
      cell: (c) => PRIORIDADE_LABELS[c.prioridade],
      className: 'w-[120px]',
    },
    {
      key: 'ativo',
      header: 'Ativa',
      cell: (c) => (
        <Switch
          checked={c.ativo}
          disabled={togglarMut.variables?.id === c.id}
          onCheckedChange={() => togglarMut.mutate(c)}
        />
      ),
      className: 'w-[80px]',
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[120px]',
      cell: (c) => (
        <Button variant="ghost" size="sm" onClick={() => { setEditing(c); setDialogOpen(true); }}>
          <Pencil className="h-4 w-4" /> Editar
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button onClick={() => { setEditing(null); setDialogOpen(true); }}>
          <Plus className="h-4 w-4" /> Nova configuração
        </Button>
      </div>

      <DataTable
        data={configsQuery.data}
        columns={columns}
        isLoading={configsQuery.isLoading}
        isError={configsQuery.isError}
        rowKey={(c) => c.id}
        emptyState={
          <div className="space-y-3">
            <p>Nenhuma configuração cadastrada.</p>
            <Button variant="outline" onClick={() => { setEditing(null); setDialogOpen(true); }}>
              <Plus className="h-4 w-4" /> Criar primeira configuração
            </Button>
          </div>
        }
      />

      <ConfigDialog open={dialogOpen} onOpenChange={setDialogOpen} config={editing} />
    </div>
  );
}

// ─────────────────────────── Diálogo de Config ───────────────────────────

const configSchema = z.object({
  tipo: z.enum(['ESTOQUE_MINIMO_PERCENTUAL', 'ESTOQUE_MINIMO_ABSOLUTO', 'VENCIMENTO_PROXIMO_DIAS', 'RUPTURA']),
  insumoId: z.string(),
  filialId: z.string(),
  threshold: z.string().optional(),
  prioridade: z.enum(['BAIXA', 'MEDIA', 'ALTA', 'CRITICA']),
  observacao: z.string().optional(),
  ativo: z.boolean(),
});

type ConfigValues = z.infer<typeof configSchema>;

interface ConfigDialogProps {
  open: boolean;
  onOpenChange: (b: boolean) => void;
  config: AlertaConfig | null;
}

function ConfigDialog({ open, onOpenChange, config }: ConfigDialogProps) {
  const queryClient = useQueryClient();
  const isEdit = config !== null;
  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const filiaisQuery = useQuery({ queryKey: ['filiais'], queryFn: () => listarFiliais() });

  const form = useForm<ConfigValues>({
    resolver: zodResolver(configSchema),
    defaultValues: {
      tipo: 'ESTOQUE_MINIMO_ABSOLUTO',
      insumoId: SCOPE_REDE,
      filialId: SCOPE_REDE,
      threshold: '',
      prioridade: 'MEDIA',
      observacao: '',
      ativo: true,
    },
  });

  useEffect(() => {
    if (open && config) {
      form.reset({
        tipo: config.tipo,
        insumoId: config.insumoId ?? SCOPE_REDE,
        filialId: config.filialId ?? SCOPE_REDE,
        threshold: config.threshold?.toString() ?? '',
        prioridade: config.prioridade,
        observacao: config.observacao ?? '',
        ativo: config.ativo,
      });
    } else if (open && !config) {
      form.reset({
        tipo: 'ESTOQUE_MINIMO_ABSOLUTO',
        insumoId: SCOPE_REDE,
        filialId: SCOPE_REDE,
        threshold: '',
        prioridade: 'MEDIA',
        observacao: '',
        ativo: true,
      });
    }
  }, [open, config, form]);

  const tipo = form.watch('tipo');
  const requerThreshold = tipo !== 'RUPTURA';

  const createMut = useMutation({
    mutationFn: (v: ConfigValues) =>
      criarConfig({
        tipo: v.tipo,
        insumoId: v.insumoId === SCOPE_REDE ? null : v.insumoId,
        filialId: v.filialId === SCOPE_REDE ? null : v.filialId,
        threshold: v.threshold ? Number(v.threshold) : null,
        prioridade: v.prioridade,
        observacao: v.observacao || undefined,
      }),
    onSuccess: () => {
      toast.success('Configuração criada');
      queryClient.invalidateQueries({ queryKey: ['alertas-config'] });
      onOpenChange(false);
    },
    onError: (e) => toastError('Não foi possível criar', e),
  });

  const updateMut = useMutation({
    mutationFn: (v: ConfigValues) =>
      atualizarConfig(config!.id, {
        threshold: v.threshold ? Number(v.threshold) : null,
        prioridade: v.prioridade,
        observacao: v.observacao || null,
        ativo: v.ativo,
      }),
    onSuccess: () => {
      toast.success('Configuração atualizada');
      queryClient.invalidateQueries({ queryKey: ['alertas-config'] });
      onOpenChange(false);
    },
    onError: (e) => toastError('Não foi possível atualizar', e),
  });

  const onSubmit = (values: ConfigValues) => {
    if (isEdit) updateMut.mutate(values);
    else createMut.mutate(values);
  };

  const isPending = createMut.isPending || updateMut.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar configuração' : 'Nova configuração de alerta'}</DialogTitle>
          <DialogDescription>
            Escolha tipo + escopo (insumo e/ou filial específicos, ou rede toda) + threshold.
            Configurações inativas não disparam alertas.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="grid gap-3 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="tipo">Tipo</Label>
              <Select
                value={form.watch('tipo')}
                onValueChange={(v) => form.setValue('tipo', v as TipoAlerta)}
                disabled={isEdit}
              >
                <SelectTrigger id="tipo">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(TIPO_LABELS) as TipoAlerta[]).map((t) => (
                    <SelectItem key={t} value={t}>
                      {TIPO_LABELS[t]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {isEdit && (
                <p className="text-xs text-muted-foreground">Tipo é imutável após cadastro.</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="prioridade">Prioridade</Label>
              <Select
                value={form.watch('prioridade')}
                onValueChange={(v) => form.setValue('prioridade', v as Prioridade)}
              >
                <SelectTrigger id="prioridade">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(PRIORIDADE_LABELS) as Prioridade[]).map((p) => (
                    <SelectItem key={p} value={p}>
                      {PRIORIDADE_LABELS[p]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid gap-3 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="insumoId">Insumo</Label>
              <Select
                value={form.watch('insumoId')}
                onValueChange={(v) => form.setValue('insumoId', v)}
                disabled={isEdit}
              >
                <SelectTrigger id="insumoId">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={SCOPE_REDE}>Todos os insumos</SelectItem>
                  {insumosQuery.data?.map((i) => (
                    <SelectItem key={i.id} value={i.id}>
                      {i.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="filialId">Filial</Label>
              <Select
                value={form.watch('filialId')}
                onValueChange={(v) => form.setValue('filialId', v)}
                disabled={isEdit}
              >
                <SelectTrigger id="filialId">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={SCOPE_REDE}>Toda a rede</SelectItem>
                  {filiaisQuery.data?.filter((f) => f.ativa).map((f) => (
                    <SelectItem key={f.id} value={f.id}>
                      {f.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {requerThreshold && (
            <div className="space-y-2">
              <Label htmlFor="threshold">
                Threshold {tipo === 'ESTOQUE_MINIMO_PERCENTUAL' && '(% — 0 a 100)'}
                {tipo === 'ESTOQUE_MINIMO_ABSOLUTO' && '(qtd na unidade base)'}
                {tipo === 'VENCIMENTO_PROXIMO_DIAS' && '(dias antes do vencimento)'}
              </Label>
              <Input id="threshold" type="number" step="0.01" min="0" {...form.register('threshold')} />
            </div>
          )}

          {isEdit && (
            <div className="flex items-center justify-between rounded-md border p-3">
              <div>
                <Label className="text-sm font-medium">Configuração ativa</Label>
                <p className="text-xs text-muted-foreground">
                  Configurações inativas não geram disparos novos.
                </p>
              </div>
              <Switch
                checked={form.watch('ativo')}
                onCheckedChange={(v) => form.setValue('ativo', v)}
              />
            </div>
          )}

          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? 'Salvando…' : isEdit ? 'Salvar' : 'Criar'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
