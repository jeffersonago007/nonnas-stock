import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDownToLine, ArrowUpFromLine, History, Save } from 'lucide-react';
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
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';

import { listarInsumos, listarUnidades } from '@/features/cadastros/insumos/api';
import { listarFornecedores } from '@/features/cadastros/fornecedores/api';
import { useAuthStore } from '@/features/auth/store';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import {
  type MovimentacaoResumo,
  TIPOS_ENTRADA_MANUAL,
  TIPOS_SAIDA_MANUAL,
  lancarEntradaManual,
  lancarSaidaManual,
  listarMovimentacoesPorPeriodo,
} from './api';

function formatTipoLabel(t: string) {
  return t.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}

export function MovimentacoesPage() {
  const filialId = useFilialFiltroStore((s) => s.filialId);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Movimentações"
        description="Lançamento manual de entradas e saídas, e histórico agregado por período."
      />

      <Tabs defaultValue="entrada" className="space-y-4">
        <TabsList>
          <TabsTrigger value="entrada">
            <ArrowDownToLine className="mr-2 h-4 w-4" /> Entrada
          </TabsTrigger>
          <TabsTrigger value="saida">
            <ArrowUpFromLine className="mr-2 h-4 w-4" /> Saída
          </TabsTrigger>
          <TabsTrigger value="historico">
            <History className="mr-2 h-4 w-4" /> Histórico
          </TabsTrigger>
        </TabsList>
        <TabsContent value="entrada">
          <EntradaForm filialId={filialId} />
        </TabsContent>
        <TabsContent value="saida">
          <SaidaForm filialId={filialId} />
        </TabsContent>
        <TabsContent value="historico">
          <HistoricoTab filialId={filialId} />
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ───────────────────────────── Entrada Manual ────────────────────────────

const entradaSchema = z.object({
  insumoId: z.string().uuid('Selecione um insumo'),
  unidadeLancamentoId: z.string().uuid('Selecione a unidade'),
  quantidadeLancada: z.coerce.number().positive('Informe a quantidade lançada'),
  quantidadeBase: z.coerce.number().positive('Informe a quantidade base'),
  valorUnitario: z.coerce.number().nonnegative('Valor unitário não pode ser negativo'),
  tipo: z.enum(['ENTRADA_NF', 'ENTRADA_AJUSTE', 'ENTRADA_DEVOLUCAO_CLIENTE']),
  fornecedorId: z.string().optional(),
  numeroLote: z.string().optional(),
  dataFabricacao: z.string().optional(),
  dataValidade: z.string().optional(),
  observacao: z.string().optional(),
});

type EntradaForm = z.infer<typeof entradaSchema>;

interface FormProps {
  filialId: string | null;
}

function EntradaForm({ filialId }: FormProps) {
  const queryClient = useQueryClient();
  const usuarioId = useAuthStore((s) => s.user?.id);
  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
  });
  const fornecedoresQuery = useQuery({
    queryKey: ['fornecedores', { ativo: true }],
    queryFn: () => listarFornecedores({ ativo: true }),
  });

  const form = useForm<EntradaForm>({
    resolver: zodResolver(entradaSchema),
    defaultValues: {
      insumoId: '',
      unidadeLancamentoId: '',
      quantidadeLancada: 0,
      quantidadeBase: 0,
      valorUnitario: 0,
      tipo: 'ENTRADA_NF',
      fornecedorId: '',
      numeroLote: '',
      dataFabricacao: '',
      dataValidade: '',
      observacao: '',
    },
  });

  const mutation = useMutation({
    mutationFn: lancarEntradaManual,
    onSuccess: () => {
      toast.success('Entrada registrada — saldo atualizado');
      queryClient.invalidateQueries({ queryKey: ['posicao'] });
      queryClient.invalidateQueries({ queryKey: ['ruptura'] });
      queryClient.invalidateQueries({ queryKey: ['vencimento'] });
      queryClient.invalidateQueries({ queryKey: ['mov-historico'] });
      form.reset();
    },
    onError: (error) => toastError('Não foi possível registrar a entrada', error),
  });

  if (!filialId) return <FilialRequiredCard verbo="lançar entradas" />;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Lançar entrada manual</CardTitle>
        <CardDescription>
          Para entradas vinculadas a NF, informe número de lote e validade quando o insumo
          controlar lote/validade.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form
          onSubmit={form.handleSubmit((values) =>
            mutation.mutate({
              filialId,
              usuarioId: usuarioId ?? '',
              insumoId: values.insumoId,
              unidadeLancamentoId: values.unidadeLancamentoId,
              quantidadeLancada: values.quantidadeLancada,
              quantidadeBase: values.quantidadeBase,
              valorUnitario: values.valorUnitario,
              tipo: values.tipo,
              fornecedorId: values.fornecedorId || null,
              numeroLote: values.numeroLote || null,
              dataFabricacao: values.dataFabricacao || null,
              dataValidade: values.dataValidade || null,
              observacao: values.observacao || undefined,
            }),
          )}
          className="grid gap-4 md:grid-cols-2"
          noValidate
        >
          <SelectField
            id="insumoId"
            label="Insumo"
            value={form.watch('insumoId')}
            onChange={(v) => form.setValue('insumoId', v, { shouldValidate: true })}
            error={form.formState.errors.insumoId?.message}
            options={insumosQuery.data?.map((i) => ({ value: i.id, label: `${i.nome} (${i.codigo})` })) ?? []}
            placeholder={insumosQuery.isLoading ? 'Carregando…' : 'Selecione…'}
          />
          <SelectField
            id="unidadeLancamentoId"
            label="Unidade lançada"
            value={form.watch('unidadeLancamentoId')}
            onChange={(v) => form.setValue('unidadeLancamentoId', v, { shouldValidate: true })}
            error={form.formState.errors.unidadeLancamentoId?.message}
            options={unidadesQuery.data?.map((u) => ({ value: u.id, label: `${u.codigo} — ${u.nome}` })) ?? []}
            placeholder="Selecione…"
          />
          <NumField
            label="Quantidade lançada"
            register={form.register('quantidadeLancada')}
            error={form.formState.errors.quantidadeLancada?.message}
            step="1"
          />
          <NumField
            label="Quantidade base (após conversão)"
            register={form.register('quantidadeBase')}
            error={form.formState.errors.quantidadeBase?.message}
            step="1"
          />
          <NumField
            label="Valor unitário (R$)"
            register={form.register('valorUnitario')}
            error={form.formState.errors.valorUnitario?.message}
            step="0.01"
          />
          <SelectField
            id="tipo"
            label="Tipo"
            value={form.watch('tipo')}
            onChange={(v) => form.setValue('tipo', v as never)}
            options={TIPOS_ENTRADA_MANUAL.map((t) => ({ value: t, label: formatTipoLabel(t) }))}
          />
          <SelectField
            id="fornecedorId"
            label="Fornecedor (opcional)"
            value={form.watch('fornecedorId') || '__sem__'}
            onChange={(v) => form.setValue('fornecedorId', v === '__sem__' ? '' : v)}
            options={[
              { value: '__sem__', label: 'Sem fornecedor' },
              ...(fornecedoresQuery.data?.map((f) => ({ value: f.id, label: f.razaoSocial })) ?? []),
            ]}
          />
          <div className="space-y-2">
            <Label htmlFor="numeroLote">Lote</Label>
            <Input id="numeroLote" {...form.register('numeroLote')} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="dataFabricacao">Data fabricação</Label>
            <Input id="dataFabricacao" type="date" {...form.register('dataFabricacao')} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="dataValidade">Data validade</Label>
            <Input id="dataValidade" type="date" {...form.register('dataValidade')} />
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="observacao">Observação</Label>
            <Textarea id="observacao" rows={2} {...form.register('observacao')} />
          </div>
          <div className="md:col-span-2 flex justify-end">
            <Button type="submit" disabled={mutation.isPending}>
              <Save className="h-4 w-4" />
              {mutation.isPending ? 'Registrando…' : 'Registrar entrada'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

// ────────────────────────────── Saída Manual ─────────────────────────────

const saidaSchema = z.object({
  insumoId: z.string().uuid('Selecione um insumo'),
  unidadeLancamentoId: z.string().uuid('Selecione a unidade'),
  quantidadeBase: z.coerce.number().positive('Informe a quantidade base'),
  tipo: z.enum(['SAIDA_AJUSTE', 'SAIDA_PERDA', 'SAIDA_QUEBRA', 'SAIDA_VENCIMENTO']),
  observacao: z.string().optional(),
});

type SaidaForm = z.infer<typeof saidaSchema>;

function SaidaForm({ filialId }: FormProps) {
  const queryClient = useQueryClient();
  const usuarioId = useAuthStore((s) => s.user?.id);
  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
  });

  const form = useForm<SaidaForm>({
    resolver: zodResolver(saidaSchema),
    defaultValues: {
      insumoId: '',
      unidadeLancamentoId: '',
      quantidadeBase: 0,
      tipo: 'SAIDA_AJUSTE',
      observacao: '',
    },
  });

  const mutation = useMutation({
    mutationFn: lancarSaidaManual,
    onSuccess: () => {
      toast.success('Saída registrada — saldo atualizado');
      queryClient.invalidateQueries({ queryKey: ['posicao'] });
      queryClient.invalidateQueries({ queryKey: ['ruptura'] });
      queryClient.invalidateQueries({ queryKey: ['vencimento'] });
      queryClient.invalidateQueries({ queryKey: ['mov-historico'] });
      form.reset();
    },
    onError: (error) => toastError('Não foi possível registrar a saída', error),
  });

  if (!filialId) return <FilialRequiredCard verbo="lançar saídas" />;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Lançar saída manual</CardTitle>
        <CardDescription>
          Saídas por venda são registradas via integração com canais de venda. Use este formulário
          para ajustes, perdas e vencimentos.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form
          onSubmit={form.handleSubmit((values) =>
            mutation.mutate({
              filialId,
              usuarioId: usuarioId ?? '',
              insumoId: values.insumoId,
              unidadeLancamentoId: values.unidadeLancamentoId,
              quantidadeBase: values.quantidadeBase,
              tipo: values.tipo,
              observacao: values.observacao || undefined,
            }),
          )}
          className="grid gap-4 md:grid-cols-2"
          noValidate
        >
          <SelectField
            id="insumoId"
            label="Insumo"
            value={form.watch('insumoId')}
            onChange={(v) => form.setValue('insumoId', v, { shouldValidate: true })}
            error={form.formState.errors.insumoId?.message}
            options={insumosQuery.data?.map((i) => ({ value: i.id, label: `${i.nome} (${i.codigo})` })) ?? []}
            placeholder={insumosQuery.isLoading ? 'Carregando…' : 'Selecione…'}
          />
          <SelectField
            id="unidadeLancamentoId"
            label="Unidade"
            value={form.watch('unidadeLancamentoId')}
            onChange={(v) => form.setValue('unidadeLancamentoId', v, { shouldValidate: true })}
            error={form.formState.errors.unidadeLancamentoId?.message}
            options={unidadesQuery.data?.map((u) => ({ value: u.id, label: `${u.codigo} — ${u.nome}` })) ?? []}
            placeholder="Selecione…"
          />
          <NumField
            label="Quantidade base"
            register={form.register('quantidadeBase')}
            error={form.formState.errors.quantidadeBase?.message}
            step="1"
          />
          <SelectField
            id="tipo"
            label="Tipo"
            value={form.watch('tipo')}
            onChange={(v) => form.setValue('tipo', v as never)}
            options={TIPOS_SAIDA_MANUAL.map((t) => ({ value: t, label: formatTipoLabel(t) }))}
          />
          <div className="md:col-span-2 space-y-2">
            <Label htmlFor="observacao">Observação</Label>
            <Textarea id="observacao" rows={2} {...form.register('observacao')} />
          </div>
          <div className="md:col-span-2 flex justify-end">
            <Button type="submit" disabled={mutation.isPending}>
              <Save className="h-4 w-4" />
              {mutation.isPending ? 'Registrando…' : 'Registrar saída'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

// ───────────────────────────── Histórico Tab ─────────────────────────────

function HistoricoTab({ filialId }: FormProps) {
  const hoje = new Date().toISOString().slice(0, 10);
  const seteDiasAtras = new Date(Date.now() - 7 * 24 * 3600 * 1000).toISOString().slice(0, 10);
  const [inicio, setInicio] = useState(seteDiasAtras);
  const [fim, setFim] = useState(hoje);

  const inicioIso = useMemo(() => `${inicio}T00:00:00Z`, [inicio]);
  const fimIso = useMemo(() => `${fim}T23:59:59Z`, [fim]);

  const query = useQuery({
    queryKey: ['mov-historico', { filialId, inicioIso, fimIso }],
    queryFn: () =>
      listarMovimentacoesPorPeriodo({ filialId, inicio: inicioIso, fim: fimIso }),
    enabled: Boolean(inicio && fim),
  });

  const columns: ColumnDef<MovimentacaoResumo>[] = [
    { key: 'codigo', header: 'Código', cell: (m) => <code className="text-xs">{m.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Insumo', cell: (m) => <span className="font-medium">{m.nome}</span> },
    { key: 'tipo', header: 'Tipo', cell: (m) => formatTipoLabel(m.tipoMovimentacao) },
    {
      key: 'qtd',
      header: 'Movimentações',
      cell: (m) => <span className="font-mono text-muted-foreground">{m.quantidadeMovimentacoes}</span>,
      className: 'text-right w-[120px]',
    },
    {
      key: 'qtdTotal',
      header: 'Qtd total',
      cell: (m) => <span className="font-mono">{m.quantidadeTotal.toLocaleString('pt-BR')}</span>,
      className: 'text-right w-[140px]',
    },
    {
      key: 'valorTotal',
      header: 'Valor total',
      cell: (m) => (
        <span className="font-mono">
          {m.valorTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
        </span>
      ),
      className: 'text-right w-[160px]',
    },
  ];

  return (
    <div className="space-y-4">
      <div className="grid gap-3 rounded-md border bg-card p-4 md:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="inicio">Início</Label>
          <Input id="inicio" type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="fim">Fim</Label>
          <Input id="fim" type="date" value={fim} onChange={(e) => setFim(e.target.value)} />
        </div>
      </div>

      <DataTable
        data={query.data}
        columns={columns}
        isLoading={query.isLoading}
        isError={query.isError}
        rowKey={(m) => `${m.filialId}|${m.insumoId}|${m.tipoMovimentacao}`}
        emptyState={<p>Nenhuma movimentação no período selecionado.</p>}
      />
    </div>
  );
}

// ────────────────────────────── Helpers ──────────────────────────────────

interface SelectFieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: Array<{ value: string; label: string }>;
  placeholder?: string;
  error?: string;
}

function SelectField({ id, label, value, onChange, options, placeholder, error }: SelectFieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Select value={value} onValueChange={onChange}>
        <SelectTrigger id={id}>
          <SelectValue placeholder={placeholder ?? 'Selecione…'} />
        </SelectTrigger>
        <SelectContent>
          {options.map((o) => (
            <SelectItem key={o.value} value={o.value}>
              {o.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

interface NumFieldProps {
  label: string;
  register: ReturnType<ReturnType<typeof useForm>['register']>;
  error?: string;
  step?: string;
}

function NumField({ label, register, error, step = '1' }: NumFieldProps) {
  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      <Input type="number" step={step} min="0" {...register} />
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

function FilialRequiredCard({ verbo }: { verbo: string }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Selecione uma filial</CardTitle>
        <CardDescription>
          Para {verbo}, escolha uma filial específica no filtro do header. O lançamento manual
          sempre se aplica a uma filial só.
        </CardDescription>
      </CardHeader>
    </Card>
  );
}
