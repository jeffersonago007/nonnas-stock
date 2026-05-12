import { useMemo, useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, Save, Trash2, Upload } from 'lucide-react';
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
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { PageHeader } from '@/components/PageHeader';
import { toastError } from '@/lib/toastError';
import { useAuthStore } from '@/features/auth/store';

import { listarInsumos, listarUnidades } from '@/features/cadastros/insumos/api';
import { buscarFilial } from './api';
import {
  type CargaPreview,
  type CargaPreviewLinha,
  confirmarCargaInicial,
  montarCsvLinhaALinha,
  previewCargaInicial,
} from './cargaInicialApi';

type Modo = 'planilha' | 'linha-a-linha';

interface ManualForm {
  itens: ManualItem[];
}

interface ManualItem {
  insumoId: string;
  unidadeId: string;
  numeroLote: string;
  quantidade: string;
  valorUnitario: string;
  dataFabricacao: string;
  dataValidade: string;
}

const linhaPadrao: ManualItem = {
  insumoId: '',
  unidadeId: '',
  numeroLote: '',
  quantidade: '',
  valorUnitario: '',
  dataFabricacao: '',
  dataValidade: '',
};

export function CargaInicialPage() {
  const { id: filialId = '' } = useParams<{ id: string }>();
  const usuarioId = useAuthStore((s) => s.user?.id);

  const filialQuery = useQuery({
    queryKey: ['filial', filialId],
    queryFn: () => buscarFilial(filialId),
    enabled: Boolean(filialId),
  });

  const [modo, setModo] = useState<Modo>('planilha');
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [preview, setPreview] = useState<CargaPreview | null>(null);

  const previewMutation = useMutation({
    mutationFn: previewCargaInicial,
    onSuccess: (data) => {
      setPreview(data);
      toast.success(`Preview gerado com ${data.totalLinhas} linha${data.totalLinhas === 1 ? '' : 's'}`);
    },
    onError: (error) => toastError('Não foi possível gerar o preview', error),
  });

  const confirmarMutation = useMutation({
    mutationFn: ({ file }: { file: File }) =>
      confirmarCargaInicial(filialId, usuarioId ?? '', file),
    onSuccess: (resultado) => {
      toast.success(
        `Carga inicial concluída: ${resultado.registrosProcessados} processados, ${resultado.registrosFalhos} falhas.`,
      );
      setArquivo(null);
      setPreview(null);
    },
    onError: (error) => toastError('Não foi possível processar a carga', error),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Carga inicial${filialQuery.data ? ` — ${filialQuery.data.nome}` : ''}`}
        description="Lance os saldos iniciais de estoque desta filial. Use upload de planilha (xlsx/csv) ou cadastro linha a linha."
        actions={
          <Button asChild variant="outline">
            <Link to="/filiais">
              <ArrowLeft className="h-4 w-4" /> Voltar para filiais
            </Link>
          </Button>
        }
      />

      <div className="flex gap-2 rounded-md border bg-card p-1 w-fit">
        <ModoButton modo={modo} value="planilha" onClick={setModo}>
          Upload de planilha
        </ModoButton>
        <ModoButton modo={modo} value="linha-a-linha" onClick={setModo}>
          Linha a linha
        </ModoButton>
      </div>

      {modo === 'planilha' ? (
        <PlanilhaUpload
          arquivo={arquivo}
          setArquivo={setArquivo}
          preview={preview}
          setPreview={setPreview}
          onPreview={(file) => previewMutation.mutate(file)}
          isPreviewing={previewMutation.isPending}
          onConfirmar={(file) => confirmarMutation.mutate({ file })}
          isConfirming={confirmarMutation.isPending}
        />
      ) : (
        <LinhaALinha
          onConfirmar={(file) => confirmarMutation.mutate({ file })}
          isConfirming={confirmarMutation.isPending}
        />
      )}
    </div>
  );
}

interface ModoButtonProps {
  modo: Modo;
  value: Modo;
  onClick: (m: Modo) => void;
  children: React.ReactNode;
}

function ModoButton({ modo, value, onClick, children }: ModoButtonProps) {
  const active = modo === value;
  return (
    <button
      type="button"
      onClick={() => onClick(value)}
      className={
        active
          ? 'rounded px-3 py-1.5 text-sm font-medium bg-primary text-primary-foreground'
          : 'rounded px-3 py-1.5 text-sm font-medium text-muted-foreground hover:text-foreground'
      }
    >
      {children}
    </button>
  );
}

interface PlanilhaProps {
  arquivo: File | null;
  setArquivo: (f: File | null) => void;
  preview: CargaPreview | null;
  setPreview: (p: CargaPreview | null) => void;
  onPreview: (file: File) => void;
  isPreviewing: boolean;
  onConfirmar: (file: File) => void;
  isConfirming: boolean;
}

function PlanilhaUpload({
  arquivo,
  setArquivo,
  preview,
  setPreview,
  onPreview,
  isPreviewing,
  onConfirmar,
  isConfirming,
}: PlanilhaProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Upload de planilha</CardTitle>
        <CardDescription>
          Arquivo .xlsx ou .csv. Colunas esperadas: insumo_id, unidade_id, numero_lote,
          quantidade, valor_unitario, data_fabricacao, data_validade. O preview abaixo permite
          conferir antes de confirmar.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-end gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="planilha">Arquivo</Label>
            <Input
              id="planilha"
              type="file"
              accept=".xlsx,.csv"
              onChange={(e) => {
                setArquivo(e.target.files?.[0] ?? null);
                setPreview(null);
              }}
            />
          </div>
          <Button
            type="button"
            disabled={!arquivo || isPreviewing}
            onClick={() => arquivo && onPreview(arquivo)}
          >
            <Upload className="h-4 w-4" />
            {isPreviewing ? 'Gerando…' : 'Gerar preview'}
          </Button>
        </div>

        {preview && (
          <div className="space-y-3">
            <div className="rounded-md border bg-muted/30 p-3 text-sm">
              <strong>{preview.totalLinhas}</strong> linha{preview.totalLinhas === 1 ? '' : 's'}{' '}
              prontas para importação. Hash:{' '}
              <code className="text-xs">{preview.hashPlanilha.slice(0, 16)}…</code>
            </div>
            <PreviewTable linhas={preview.linhas} />
            <div className="flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setArquivo(null);
                  setPreview(null);
                }}
              >
                Cancelar
              </Button>
              <Button
                type="button"
                disabled={!arquivo || isConfirming}
                onClick={() => arquivo && onConfirmar(arquivo)}
              >
                <ArrowRight className="h-4 w-4" />
                {isConfirming ? 'Importando…' : 'Confirmar importação'}
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

interface LinhaALinhaProps {
  onConfirmar: (file: File) => void;
  isConfirming: boolean;
}

function LinhaALinha({ onConfirmar, isConfirming }: LinhaALinhaProps) {
  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
  });

  const form = useForm<ManualForm>({
    defaultValues: { itens: [linhaPadrao] },
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: 'itens' });

  const linhasMemo = useMemo<CargaPreviewLinha[]>(() => {
    return form.watch('itens').map((it, idx) => ({
      numeroLinha: idx + 1,
      insumoId: it.insumoId,
      unidadeId: it.unidadeId,
      numeroLote: it.numeroLote,
      quantidade: Number(it.quantidade) || 0,
      valorUnitario: Number(it.valorUnitario) || 0,
      dataFabricacao: it.dataFabricacao || null,
      dataValidade: it.dataValidade || null,
    }));
    // Reagindo ao form via watch — deps lint pode reclamar, mas o watch já
    // chama o subscriber e reflete novos valores em cada render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.watch('itens')]);

  const onSubmit = form.handleSubmit(() => {
    const incompletos = linhasMemo.some(
      (l) => !l.insumoId || !l.unidadeId || !l.numeroLote || l.quantidade <= 0 || l.valorUnitario <= 0,
    );
    if (incompletos) {
      toast.error('Preencha todos os campos obrigatórios em cada linha');
      return;
    }
    const file = montarCsvLinhaALinha(linhasMemo);
    onConfirmar(file);
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Cadastro linha a linha</CardTitle>
        <CardDescription>
          Para volumes pequenos, monte os lotes manualmente. As linhas viram um CSV equivalente
          ao upload.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={onSubmit} className="space-y-3">
          {fields.map((field, index) => (
            <div
              key={field.id}
              className="grid gap-2 rounded-md border p-3 sm:grid-cols-[2fr_1fr_1fr_1fr_1fr_1fr_1fr_auto]"
            >
              <div className="space-y-1.5">
                {index === 0 && <Label className="text-xs">Insumo</Label>}
                <Select
                  value={form.watch(`itens.${index}.insumoId`)}
                  onValueChange={(v) => form.setValue(`itens.${index}.insumoId`, v)}
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
              </div>
              <div className="space-y-1.5">
                {index === 0 && <Label className="text-xs">Unidade</Label>}
                <Select
                  value={form.watch(`itens.${index}.unidadeId`)}
                  onValueChange={(v) => form.setValue(`itens.${index}.unidadeId`, v)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Un" />
                  </SelectTrigger>
                  <SelectContent>
                    {unidadesQuery.data?.filter((u) => u.ativa).map((u) => (
                      <SelectItem key={u.id} value={u.id}>
                        {u.codigo}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                {index === 0 && <Label className="text-xs">Lote</Label>}
                <Input {...form.register(`itens.${index}.numeroLote`)} placeholder="LOTE-001" />
              </div>
              <div className="space-y-1.5">
                {index === 0 && <Label className="text-xs">Qtde</Label>}
                <Input
                  type="number"
                  step="0.001"
                  min="0"
                  {...form.register(`itens.${index}.quantidade`)}
                />
              </div>
              <div className="space-y-1.5">
                {index === 0 && <Label className="text-xs">Valor un.</Label>}
                <Input
                  type="number"
                  step="0.01"
                  min="0"
                  {...form.register(`itens.${index}.valorUnitario`)}
                />
              </div>
              <div className="space-y-1.5">
                {index === 0 && <Label className="text-xs">Fabricação</Label>}
                <Input type="date" {...form.register(`itens.${index}.dataFabricacao`)} />
              </div>
              <div className="space-y-1.5">
                {index === 0 && <Label className="text-xs">Validade</Label>}
                <Input type="date" {...form.register(`itens.${index}.dataValidade`)} />
              </div>
              <div className="flex items-end">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => remove(index)}
                  disabled={fields.length === 1}
                  aria-label="Remover linha"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </div>
          ))}

          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="outline" onClick={() => append(linhaPadrao)}>
              <Plus className="h-4 w-4" /> Adicionar linha
            </Button>
            <Button type="submit" disabled={isConfirming}>
              <Save className="h-4 w-4" />
              {isConfirming ? 'Importando…' : 'Importar lotes'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

function PreviewTable({ linhas }: { linhas: CargaPreviewLinha[] }) {
  if (linhas.length === 0) return null;
  return (
    <div className="rounded-md border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-12">#</TableHead>
            <TableHead>Insumo</TableHead>
            <TableHead>Unidade</TableHead>
            <TableHead>Lote</TableHead>
            <TableHead>Qtde</TableHead>
            <TableHead>Valor un.</TableHead>
            <TableHead>Validade</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {linhas.slice(0, 50).map((l) => (
            <TableRow key={l.numeroLinha}>
              <TableCell className="text-muted-foreground">{l.numeroLinha}</TableCell>
              <TableCell className="font-mono text-xs">{l.insumoId.slice(0, 8)}…</TableCell>
              <TableCell className="font-mono text-xs">{l.unidadeId.slice(0, 8)}…</TableCell>
              <TableCell>{l.numeroLote}</TableCell>
              <TableCell>{l.quantidade}</TableCell>
              <TableCell>{l.valorUnitario}</TableCell>
              <TableCell className="text-xs">{l.dataValidade ?? '—'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {linhas.length > 50 && (
        <p className="border-t p-2 text-center text-xs text-muted-foreground">
          Mostrando primeiras 50 de {linhas.length} linhas.
        </p>
      )}
    </div>
  );
}
