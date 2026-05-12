import { useEffect, useMemo } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { Plus, Save, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { z } from 'zod';

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
import { PageHeader } from '@/components/PageHeader';
import { toastError } from '@/lib/toastError';

import { listarProdutos } from '@/features/cadastros/produtos/api';
import { listarInsumos, listarUnidades } from '@/features/cadastros/insumos/api';
import {
  type FichaTecnica,
  atualizarFichaVigente,
  buscarFichaVigente,
  criarFicha,
  listarHistoricoFichas,
} from './api';

const itemSchema = z.object({
  insumoId: z.string().uuid('Selecione um insumo'),
  unidadeId: z.string().uuid('Selecione a unidade'),
  quantidade: z.coerce.number().positive('Quantidade deve ser positiva'),
});

const fichaSchema = z.object({
  itens: z.array(itemSchema).min(1, 'Inclua ao menos um insumo'),
});

type FichaValues = z.infer<typeof fichaSchema>;

export function FichasTecnicasPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const produtoId = searchParams.get('produtoId') ?? '';
  const queryClient = useQueryClient();

  const produtosQuery = useQuery({
    queryKey: ['produtos', { ativo: true, tipo: 'FABRICADO' }],
    queryFn: () => listarProdutos({ ativo: true, tipo: 'FABRICADO' }),
  });

  const fichaVigenteQuery = useQuery({
    queryKey: ['ficha-vigente', produtoId],
    queryFn: () => buscarFichaVigente(produtoId),
    enabled: Boolean(produtoId),
  });

  const historicoQuery = useQuery({
    queryKey: ['ficha-historico', produtoId],
    queryFn: () => listarHistoricoFichas(produtoId),
    enabled: Boolean(produtoId),
  });

  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
  });

  const insumoMap = useMemo(() => {
    const m = new Map<string, string>();
    insumosQuery.data?.forEach((i) => m.set(i.id, i.nome));
    return m;
  }, [insumosQuery.data]);
  const unidadeMap = useMemo(() => {
    const m = new Map<string, string>();
    unidadesQuery.data?.forEach((u) => m.set(u.id, u.codigo));
    return m;
  }, [unidadesQuery.data]);

  const handleProdutoChange = (id: string) => {
    if (id) setSearchParams({ produtoId: id });
    else setSearchParams({});
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Fichas técnicas"
        description="Receita de cada produto vendável: insumos, unidades e quantidades. Cada salvar gera nova versão."
      />

      <div className="rounded-md border bg-card p-4">
        <div className="space-y-1.5 max-w-md">
          <Label htmlFor="produto">Produto</Label>
          <Select value={produtoId} onValueChange={handleProdutoChange}>
            <SelectTrigger id="produto">
              <SelectValue placeholder={produtosQuery.isLoading ? 'Carregando…' : 'Selecione um produto'} />
            </SelectTrigger>
            <SelectContent>
              {produtosQuery.data?.map((p) => (
                <SelectItem key={p.id} value={p.id}>
                  {p.nome} <span className="text-muted-foreground">({p.codigo})</span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {!produtoId && (
        <div className="rounded-md border border-dashed bg-card p-10 text-center text-sm text-muted-foreground">
          Selecione um produto vendável acima para visualizar e editar a ficha técnica.
        </div>
      )}

      {produtoId && (
        <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
          <FichaEditor
            produtoId={produtoId}
            ficha={fichaVigenteQuery.data}
            isLoading={fichaVigenteQuery.isLoading}
            onSaved={() => {
              queryClient.invalidateQueries({ queryKey: ['ficha-vigente', produtoId] });
              queryClient.invalidateQueries({ queryKey: ['ficha-historico', produtoId] });
            }}
          />
          <HistoricoVersoes
            historico={historicoQuery.data}
            isLoading={historicoQuery.isLoading}
            insumoMap={insumoMap}
            unidadeMap={unidadeMap}
          />
        </div>
      )}
    </div>
  );
}

interface EditorProps {
  produtoId: string;
  ficha: FichaTecnica | null | undefined;
  isLoading: boolean;
  onSaved: () => void;
}

function FichaEditor({ produtoId, ficha, isLoading, onSaved }: EditorProps) {
  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: true }],
    queryFn: () => listarInsumos({ ativo: true }),
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
  });

  const form = useForm<FichaValues>({
    resolver: zodResolver(fichaSchema),
    defaultValues: { itens: [{ insumoId: '', unidadeId: '', quantidade: 0 }] },
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: 'itens' });

  useEffect(() => {
    if (ficha) {
      form.reset({
        itens: ficha.itens.map((i) => ({
          insumoId: i.insumoId,
          unidadeId: i.unidadeId,
          quantidade: i.quantidade,
        })),
      });
    } else if (ficha === null) {
      form.reset({ itens: [{ insumoId: '', unidadeId: '', quantidade: 0 }] });
    }
  }, [ficha, form]);

  const mutation = useMutation({
    mutationFn: (values: FichaValues) =>
      ficha ? atualizarFichaVigente(produtoId, values) : criarFicha(produtoId, values),
    onSuccess: () => {
      toast.success(ficha ? 'Nova versão da ficha gerada' : 'Ficha técnica criada');
      onSaved();
    },
    onError: (error) => toastError('Não foi possível salvar a ficha', error),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">
          {ficha ? `Versão vigente — v${ficha.versao}` : 'Sem ficha técnica'}
        </CardTitle>
        <CardDescription>
          {ficha
            ? 'Editar gera uma nova versão; a anterior fica disponível no histórico.'
            : 'Cadastre os insumos da receita para criar a primeira versão.'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Carregando ficha…</p>
        ) : (
          <form
            className="space-y-4"
            onSubmit={form.handleSubmit((v) => mutation.mutate(v))}
            noValidate
          >
            <div className="space-y-3">
              {fields.map((field, index) => (
                <div key={field.id} className="grid gap-2 sm:grid-cols-[2fr_1fr_1fr_auto]">
                  <div className="space-y-1.5">
                    {index === 0 && <Label>Insumo</Label>}
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
                  </div>
                  <div className="space-y-1.5">
                    {index === 0 && <Label>Unidade</Label>}
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
                        {unidadesQuery.data?.filter((u) => u.ativa).map((u) => (
                          <SelectItem key={u.id} value={u.id}>
                            {u.codigo}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-1.5">
                    {index === 0 && <Label>Qtde</Label>}
                    <Input
                      type="number"
                      step="0.001"
                      min="0"
                      {...form.register(`itens.${index}.quantidade`)}
                    />
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
              {form.formState.errors.itens?.root && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.itens.root.message as string}
                </p>
              )}
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => append({ insumoId: '', unidadeId: '', quantidade: 0 })}
              >
                <Plus className="h-4 w-4" /> Adicionar insumo
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                <Save className="h-4 w-4" />
                {mutation.isPending ? 'Salvando…' : ficha ? 'Salvar nova versão' : 'Criar ficha'}
              </Button>
            </div>
          </form>
        )}
      </CardContent>
    </Card>
  );
}

interface HistoricoProps {
  historico: FichaTecnica[] | undefined;
  isLoading: boolean;
  insumoMap: Map<string, string>;
  unidadeMap: Map<string, string>;
}

function HistoricoVersoes({ historico, isLoading, insumoMap, unidadeMap }: HistoricoProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Histórico de versões</CardTitle>
        <CardDescription>Cada salvar arquiva a versão anterior.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {isLoading && <p className="text-sm text-muted-foreground">Carregando histórico…</p>}
        {!isLoading && (!historico || historico.length === 0) && (
          <p className="text-sm text-muted-foreground">Sem versões anteriores ainda.</p>
        )}
        {historico?.map((f) => (
          <div
            key={f.id}
            className="rounded-md border p-3"
            style={{ borderColor: f.ativa ? 'hsl(var(--primary))' : undefined }}
          >
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">
                v{f.versao} {f.ativa && <span className="text-primary">(vigente)</span>}
              </span>
              <span className="text-xs text-muted-foreground">
                {new Date(f.vigenteDesde).toLocaleDateString('pt-BR')}
              </span>
            </div>
            <ul className="mt-2 space-y-1 text-xs text-muted-foreground">
              {f.itens.map((item) => (
                <li key={item.id}>
                  {insumoMap.get(item.insumoId) ?? item.insumoId.slice(0, 8)} —{' '}
                  {item.quantidade} {unidadeMap.get(item.unidadeId) ?? '?'}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
