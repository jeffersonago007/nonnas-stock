import { useEffect, useMemo, useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeftRight, ArrowRight, CheckCircle, Plus, Send, Trash2, XCircle } from 'lucide-react';
import { toast } from 'sonner';
import { z } from 'zod';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
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
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';

import { listarFiliais } from '@/features/cadastros/filiais/api';
import { listarInsumos, listarUnidades } from '@/features/cadastros/insumos/api';
import { useAuthStore } from '@/features/auth/store';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import {
  type StatusTransferencia,
  type Transferencia,
  aprovarTransferencia,
  cancelarTransferencia,
  enviarTransferencia,
  listarTransferencias,
  receberTransferencia,
  solicitarTransferencia,
} from './api';

const STATUS_TODOS = '__todos__';

const STATUS_LABELS: Record<StatusTransferencia, string> = {
  SOLICITADA: 'Solicitada',
  APROVADA: 'Aprovada',
  EM_TRANSITO: 'Em trânsito',
  RECEBIDA: 'Recebida',
  CANCELADA: 'Cancelada',
};

const STATUS_TONES: Record<StatusTransferencia, string> = {
  SOLICITADA: 'bg-blue-100 text-blue-900',
  APROVADA: 'bg-amber-100 text-amber-900',
  EM_TRANSITO: 'bg-purple-100 text-purple-900',
  RECEBIDA: 'bg-secondary/10 text-secondary',
  CANCELADA: 'bg-muted text-muted-foreground',
};

export function TransferenciasPage() {
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const queryClient = useQueryClient();
  const usuarioId = useAuthStore((s) => s.user?.id);
  const [statusFiltro, setStatusFiltro] = useState(STATUS_TODOS);
  const [criarOpen, setCriarOpen] = useState(false);
  const [receberOpen, setReceberOpen] = useState<Transferencia | null>(null);
  const [cancelarOpen, setCancelarOpen] = useState<Transferencia | null>(null);

  const params = useMemo(
    () => ({
      filialId,
      status: statusFiltro === STATUS_TODOS ? null : (statusFiltro as StatusTransferencia),
    }),
    [filialId, statusFiltro],
  );

  const transferenciasQuery = useQuery({
    queryKey: ['transferencias', params],
    queryFn: () => listarTransferencias(params),
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

  const aprovarMut = useMutation({
    mutationFn: (id: string) => aprovarTransferencia(id, usuarioId ?? ''),
    onSuccess: () => {
      toast.success('Transferência aprovada');
      queryClient.invalidateQueries({ queryKey: ['transferencias'] });
    },
    onError: (e) => toastError('Não foi possível aprovar', e),
  });

  const enviarMut = useMutation({
    mutationFn: (id: string) => enviarTransferencia(id, usuarioId ?? ''),
    onSuccess: () => {
      toast.success('Envio registrado');
      queryClient.invalidateQueries({ queryKey: ['transferencias'] });
      queryClient.invalidateQueries({ queryKey: ['posicao'] });
    },
    onError: (e) => toastError('Não foi possível registrar envio', e),
  });

  const togglingId = aprovarMut.variables ?? enviarMut.variables;

  const columns: ColumnDef<Transferencia>[] = [
    {
      key: 'rota',
      header: 'Rota',
      cell: (t) => (
        <div className="flex items-center gap-1 text-sm">
          <span>{filialMap.get(t.filialOrigemId) ?? t.filialOrigemId.slice(0, 8)}</span>
          <ArrowRight className="h-3 w-3 text-muted-foreground" />
          <span>{filialMap.get(t.filialDestinoId) ?? t.filialDestinoId.slice(0, 8)}</span>
        </div>
      ),
    },
    {
      key: 'data',
      header: 'Solicitada',
      cell: (t) => (
        <span className="text-sm text-muted-foreground">
          {new Date(t.dataSolicitacao).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
        </span>
      ),
      className: 'w-[160px]',
    },
    {
      key: 'itens',
      header: 'Itens',
      cell: (t) => <span className="text-sm">{t.itens.length}</span>,
      className: 'w-[80px]',
    },
    {
      key: 'status',
      header: 'Status',
      cell: (t) => (
        <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_TONES[t.status]}`}>
          {STATUS_LABELS[t.status]}
        </span>
      ),
      className: 'w-[140px]',
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right',
      cell: (t) => (
        <div className="flex justify-end gap-2">
          {t.status === 'SOLICITADA' && (
            <>
              <Button
                size="sm"
                variant="ghost"
                disabled={togglingId === t.id}
                onClick={() => aprovarMut.mutate(t.id)}
              >
                <CheckCircle className="h-4 w-4" /> Aprovar
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setCancelarOpen(t)}
              >
                <XCircle className="h-4 w-4" /> Cancelar
              </Button>
            </>
          )}
          {t.status === 'APROVADA' && (
            <>
              <Button
                size="sm"
                variant="ghost"
                disabled={togglingId === t.id}
                onClick={() => enviarMut.mutate(t.id)}
              >
                <Send className="h-4 w-4" /> Despachar
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setCancelarOpen(t)}>
                <XCircle className="h-4 w-4" /> Cancelar
              </Button>
            </>
          )}
          {t.status === 'EM_TRANSITO' && (
            <Button size="sm" variant="ghost" onClick={() => setReceberOpen(t)}>
              <CheckCircle className="h-4 w-4" /> Receber
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Transferências"
        description="Solicite, aprove, despache e receba transferências entre filiais. State machine: Solicitada → Aprovada → Em trânsito → Recebida."
        actions={
          <Button onClick={() => setCriarOpen(true)}>
            <Plus className="h-4 w-4" /> Nova transferência
          </Button>
        }
      />

      <div className="grid gap-3 rounded-md border bg-card p-4 md:grid-cols-3">
        <div className="space-y-1.5 md:col-span-2">
          <Label>Filtro de filial</Label>
          <p className="text-sm text-muted-foreground">
            {filialId
              ? `Mostrando transferências envolvendo ${filialMap.get(filialId) ?? '(filial selecionada)'} (origem ou destino).`
              : 'Mostrando todas as transferências da rede. Selecione uma filial no header para focar.'}
          </p>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filtro-status">Status</Label>
          <Select value={statusFiltro} onValueChange={setStatusFiltro}>
            <SelectTrigger id="filtro-status">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={STATUS_TODOS}>Todos</SelectItem>
              {(Object.keys(STATUS_LABELS) as StatusTransferencia[]).map((s) => (
                <SelectItem key={s} value={s}>
                  {STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <DataTable
        data={transferenciasQuery.data}
        columns={columns}
        isLoading={transferenciasQuery.isLoading}
        isError={transferenciasQuery.isError}
        rowKey={(t) => t.id}
        emptyState={<p>Nenhuma transferência encontrada.</p>}
      />

      <CriarTransferenciaDialog
        open={criarOpen}
        onOpenChange={setCriarOpen}
        defaultFilialOrigem={filialId}
      />
      <ReceberDialog transferencia={receberOpen} onClose={() => setReceberOpen(null)} />
      <CancelarDialog transferencia={cancelarOpen} onClose={() => setCancelarOpen(null)} />
    </div>
  );
}

// ────────────────────────── Diálogo de criação ───────────────────────────

const itemSchema = z.object({
  insumoId: z.string().uuid('Selecione um insumo'),
  unidadeId: z.string().uuid('Selecione a unidade'),
  quantidade: z.coerce.number().positive('Quantidade deve ser positiva'),
});
const criarSchema = z
  .object({
    filialOrigemId: z.string().uuid('Selecione a origem'),
    filialDestinoId: z.string().uuid('Selecione o destino'),
    observacao: z.string().optional(),
    itens: z.array(itemSchema).min(1, 'Inclua ao menos um item'),
  })
  .refine((v) => v.filialOrigemId !== v.filialDestinoId, {
    message: 'Origem e destino devem ser filiais diferentes',
    path: ['filialDestinoId'],
  });
type CriarValues = z.infer<typeof criarSchema>;

interface CriarProps {
  open: boolean;
  onOpenChange: (b: boolean) => void;
  defaultFilialOrigem: string | null;
}

function CriarTransferenciaDialog({ open, onOpenChange, defaultFilialOrigem }: CriarProps) {
  const queryClient = useQueryClient();
  const usuarioId = useAuthStore((s) => s.user?.id);
  const filiaisQuery = useQuery({ queryKey: ['filiais'], queryFn: () => listarFiliais() });
  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
  });

  const form = useForm<CriarValues>({
    resolver: zodResolver(criarSchema),
    defaultValues: {
      filialOrigemId: defaultFilialOrigem ?? '',
      filialDestinoId: '',
      observacao: '',
      itens: [{ insumoId: '', unidadeId: '', quantidade: 0 }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: 'itens' });

  const filiaisAtivas = filiaisQuery.data?.filter((f) => f.ativa) ?? [];
  const origemAtual = form.watch('filialOrigemId');
  const destinoAtual = form.watch('filialDestinoId');

  useEffect(() => {
    if (open) {
      form.reset({
        filialOrigemId: defaultFilialOrigem ?? '',
        filialDestinoId: '',
        observacao: '',
        itens: [{ insumoId: '', unidadeId: '', quantidade: 0 }],
      });
    }
  }, [open, defaultFilialOrigem, form]);

  // Auto-set destino: quando há exatamente 2 filiais ativas e a origem
  // está definida, o destino é trivialmente a outra. Operador pode trocar
  // manualmente depois (ou usar o botão swap).
  useEffect(() => {
    if (!open || filiaisAtivas.length !== 2 || !origemAtual || destinoAtual) return;
    const oposto = filiaisAtivas.find((f) => f.id !== origemAtual);
    if (oposto) form.setValue('filialDestinoId', oposto.id, { shouldValidate: true });
  }, [open, filiaisAtivas, origemAtual, destinoAtual, form]);

  function trocarOrigemDestino() {
    const o = form.getValues('filialOrigemId');
    const d = form.getValues('filialDestinoId');
    form.setValue('filialOrigemId', d, { shouldValidate: true });
    form.setValue('filialDestinoId', o, { shouldValidate: true });
  }

  const mutation = useMutation({
    mutationFn: (values: CriarValues) =>
      solicitarTransferencia({
        ...values,
        solicitadoPor: usuarioId ?? '',
        observacao: values.observacao || undefined,
      }),
    onSuccess: () => {
      toast.success('Transferência solicitada');
      queryClient.invalidateQueries({ queryKey: ['transferencias'] });
      onOpenChange(false);
    },
    onError: (e) => toastError('Não foi possível solicitar a transferência', e),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle>Nova transferência</DialogTitle>
          <DialogDescription>
            A transferência começa em <strong>Solicitada</strong>. Itens irão diminuir o saldo
            da origem só após aprovação + envio.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={form.handleSubmit((v) => mutation.mutate(v))}
          className="space-y-4"
          noValidate
        >
          <div className="grid items-end gap-3 md:grid-cols-[1fr_auto_1fr]">
            <div className="space-y-2">
              <Label htmlFor="origem">Filial origem</Label>
              <Select
                value={form.watch('filialOrigemId')}
                onValueChange={(v) => form.setValue('filialOrigemId', v, { shouldValidate: true })}
              >
                <SelectTrigger id="origem">
                  <SelectValue placeholder="Selecione…" />
                </SelectTrigger>
                <SelectContent>
                  {filiaisAtivas.map((f) => (
                    <SelectItem key={f.id} value={f.id}>
                      {f.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {form.formState.errors.filialOrigemId && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.filialOrigemId.message}
                </p>
              )}
            </div>
            <div className="flex justify-center pb-1">
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={trocarOrigemDestino}
                disabled={!origemAtual || !destinoAtual}
                aria-label="Inverter origem e destino"
                title="Inverter origem e destino"
              >
                <ArrowLeftRight className="h-4 w-4" />
              </Button>
            </div>
            <div className="space-y-2">
              <Label htmlFor="destino">Filial destino</Label>
              <Select
                value={form.watch('filialDestinoId')}
                onValueChange={(v) => form.setValue('filialDestinoId', v, { shouldValidate: true })}
              >
                <SelectTrigger id="destino">
                  <SelectValue placeholder="Selecione…" />
                </SelectTrigger>
                <SelectContent>
                  {filiaisAtivas
                    .filter((f) => f.id !== form.watch('filialOrigemId'))
                    .map((f) => (
                      <SelectItem key={f.id} value={f.id}>
                        {f.nome}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
              {form.formState.errors.filialDestinoId && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.filialDestinoId.message}
                </p>
              )}
            </div>
          </div>

          <div className="space-y-3">
            <Label>Itens</Label>
            {fields.map((field, index) => (
              <div key={field.id} className="grid gap-2 sm:grid-cols-[2fr_1fr_1fr_auto]">
                <Select
                  value={form.watch(`itens.${index}.insumoId`)}
                  onValueChange={(v) =>
                    form.setValue(`itens.${index}.insumoId`, v, { shouldValidate: true })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Insumo" />
                  </SelectTrigger>
                  <SelectContent>
                    {insumosQuery.data?.map((i) => (
                      <SelectItem key={i.id} value={i.id}>
                        {i.nome}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Select
                  value={form.watch(`itens.${index}.unidadeId`)}
                  onValueChange={(v) =>
                    form.setValue(`itens.${index}.unidadeId`, v, { shouldValidate: true })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Unidade" />
                  </SelectTrigger>
                  <SelectContent>
                    {unidadesQuery.data?.map((u) => (
                      <SelectItem key={u.id} value={u.id}>
                        {u.codigo}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Input
                  type="number"
                  step="1"
                  min="0"
                  placeholder="Qtde"
                  {...form.register(`itens.${index}.quantidade`)}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => remove(index)}
                  disabled={fields.length === 1}
                  aria-label="Remover item"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              onClick={() => append({ insumoId: '', unidadeId: '', quantidade: 0 })}
            >
              <Plus className="h-4 w-4" /> Adicionar item
            </Button>
          </div>

          <div className="space-y-2">
            <Label htmlFor="observacao">Observação (opcional)</Label>
            <Textarea id="observacao" rows={2} {...form.register('observacao')} />
          </div>

          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Solicitando…' : 'Solicitar transferência'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ─────────────────────────── Diálogo de Recebimento ──────────────────────

function ReceberDialog({
  transferencia,
  onClose,
}: {
  transferencia: Transferencia | null;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const usuarioId = useAuthStore((s) => s.user?.id);
  const [quantidades, setQuantidades] = useState<Record<string, string>>({});

  useEffect(() => {
    if (transferencia) {
      const inicial: Record<string, string> = {};
      transferencia.itens.forEach((it) => {
        inicial[it.itemId] = it.quantidade.toString();
      });
      setQuantidades(inicial);
    }
  }, [transferencia]);

  const mutation = useMutation({
    mutationFn: () => {
      if (!transferencia) throw new Error('Sem transferência');
      const itens = transferencia.itens.map((it) => ({
        itemId: it.itemId,
        quantidadeRecebida: Number(quantidades[it.itemId] ?? 0),
      }));
      return receberTransferencia(transferencia.id, usuarioId ?? '', itens);
    },
    onSuccess: () => {
      toast.success('Recebimento registrado');
      queryClient.invalidateQueries({ queryKey: ['transferencias'] });
      queryClient.invalidateQueries({ queryKey: ['posicao'] });
      onClose();
    },
    onError: (e) => toastError('Não foi possível registrar recebimento', e),
  });

  return (
    <Dialog open={transferencia !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Receber transferência</DialogTitle>
          <DialogDescription>
            Confirme as quantidades efetivamente recebidas. Diferenças geram ajustes
            automáticos no estoque destino.
          </DialogDescription>
        </DialogHeader>
        {transferencia && (
          <div className="space-y-3">
            {transferencia.itens.map((it) => (
              <div key={it.itemId} className="grid gap-2 sm:grid-cols-[1fr_120px]">
                <div className="space-y-1">
                  <span className="font-mono text-xs text-muted-foreground">
                    {it.insumoId.slice(0, 8)}…
                  </span>
                  <p className="text-xs text-muted-foreground">
                    Solicitado: <strong>{it.quantidade}</strong>
                  </p>
                </div>
                <Input
                  type="number"
                  step="0.001"
                  min="0"
                  value={quantidades[it.itemId] ?? ''}
                  onChange={(e) =>
                    setQuantidades((prev) => ({ ...prev, [it.itemId]: e.target.value }))
                  }
                />
              </div>
            ))}
          </div>
        )}
        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button onClick={() => mutation.mutate()} disabled={mutation.isPending}>
            {mutation.isPending ? 'Registrando…' : 'Confirmar recebimento'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ─────────────────────────── Diálogo de Cancelamento ─────────────────────

function CancelarDialog({
  transferencia,
  onClose,
}: {
  transferencia: Transferencia | null;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const [motivo, setMotivo] = useState('');

  useEffect(() => {
    if (transferencia) setMotivo('');
  }, [transferencia]);

  const mutation = useMutation({
    mutationFn: () => {
      if (!transferencia) throw new Error('Sem transferência');
      return cancelarTransferencia(transferencia.id, motivo);
    },
    onSuccess: () => {
      toast.success('Transferência cancelada');
      queryClient.invalidateQueries({ queryKey: ['transferencias'] });
      onClose();
    },
    onError: (e) => toastError('Não foi possível cancelar', e),
  });

  return (
    <Dialog open={transferencia !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Cancelar transferência</DialogTitle>
          <DialogDescription>
            Cancelamento só é permitido antes de a transferência entrar em trânsito (já
            existem movimentações de saída).
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label htmlFor="motivo">Motivo do cancelamento</Label>
          <Textarea
            id="motivo"
            rows={3}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            placeholder="Ex.: Erro de digitação na quantidade"
          />
        </div>
        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={onClose}>
            Voltar
          </Button>
          <Button
            variant="destructive"
            disabled={mutation.isPending || motivo.trim().length === 0}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? 'Cancelando…' : 'Confirmar cancelamento'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
