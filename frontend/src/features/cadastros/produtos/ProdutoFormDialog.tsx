import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { toastError } from '@/lib/toastError';

import { type Produto, atualizarProduto, criarProduto, listarCategoriasProduto } from './api';
import { listarCategorias as listarCategoriasInsumo, listarInsumos } from '@/features/cadastros/insumos/api';

const createSchema = z
  .object({
    codigo: z.string().min(1, 'Código é obrigatório'),
    nome: z.string().min(1, 'Nome é obrigatório'),
    categoria: z.string().min(1, 'Categoria é obrigatória'),
    tipo: z.enum(['FABRICADO', 'REVENDA']),
    insumoRevendaId: z.string().optional(),
  })
  .refine((d) => d.tipo !== 'REVENDA' || !!d.insumoRevendaId, {
    message: 'Produto de revenda exige um insumo vinculado',
    path: ['insumoRevendaId'],
  });
const updateSchema = z.object({
  nome: z.string().min(1, 'Nome é obrigatório'),
  categoria: z.string().min(1, 'Categoria é obrigatória'),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  produto: Produto | null;
}

export function ProdutoFormDialog({ open, onOpenChange, produto }: Props) {
  const isEdit = produto !== null;
  const queryClient = useQueryClient();

  const categoriasProdutoQuery = useQuery({
    queryKey: ['produtos-categorias'],
    queryFn: listarCategoriasProduto,
    enabled: open,
  });
  const categoriasAdminQuery = useQuery({
    queryKey: ['categorias-insumo'],
    queryFn: listarCategoriasInsumo,
    enabled: open,
  });
  const categoriasUnificadas = (() => {
    const set = new Set<string>();
    (categoriasAdminQuery.data ?? []).filter((c) => c.ativa).forEach((c) => set.add(c.nome));
    (categoriasProdutoQuery.data ?? []).forEach((c) => set.add(c));
    return Array.from(set).sort((a, b) => a.localeCompare(b, 'pt-BR'));
  })();

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { codigo: '', nome: '', categoria: '', tipo: 'FABRICADO', insumoRevendaId: '' },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { nome: '', categoria: '' },
  });

  useEffect(() => {
    if (open && produto) {
      updateForm.reset({ nome: produto.nome, categoria: produto.categoria });
    } else if (open && !produto) {
      createForm.reset({ codigo: '', nome: '', categoria: '', tipo: 'FABRICADO', insumoRevendaId: '' });
    }
  }, [open, produto, createForm, updateForm]);

  const createMutation = useMutation({
    mutationFn: (values: CreateValues) =>
      criarProduto({
        codigo: values.codigo,
        nome: values.nome,
        categoria: values.categoria,
        tipo: values.tipo,
        insumoRevendaId: values.tipo === 'REVENDA' ? values.insumoRevendaId || null : null,
      }),
    onSuccess: () => {
      toast.success('Produto criado');
      queryClient.invalidateQueries({ queryKey: ['produtos'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar o produto', error),
  });
  const updateMutation = useMutation({
    mutationFn: (values: UpdateValues) => atualizarProduto(produto!.id, values),
    onSuccess: () => {
      toast.success('Produto atualizado');
      queryClient.invalidateQueries({ queryKey: ['produtos'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar', error),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar produto' : 'Novo produto vendável'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? `Código e tipo não podem ser alterados. Tipo atual: ${produto?.tipo === 'REVENDA' ? 'Revenda' : 'Fabricado'}.`
              : 'Fabricado usa ficha técnica (pizza, lasanha). Revenda vincula direto a 1 insumo (Coca-Cola, sorvete).'}
          </DialogDescription>
        </DialogHeader>

        {isEdit ? (
          <form
            className="space-y-4"
            onSubmit={updateForm.handleSubmit((v) => updateMutation.mutate(v))}
            noValidate
          >
            <UpdateFields form={updateForm} categorias={categoriasUnificadas} />
            <DialogFooter className="gap-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={updateMutation.isPending}>
                {updateMutation.isPending ? 'Salvando…' : 'Salvar'}
              </Button>
            </DialogFooter>
          </form>
        ) : (
          <form
            className="space-y-4"
            onSubmit={createForm.handleSubmit((v) => createMutation.mutate(v))}
            noValidate
          >
            <CreateFields form={createForm} categorias={categoriasUnificadas} />
            <DialogFooter className="gap-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Criando…' : 'Criar'}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}

interface CreateFieldsProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any;
  categorias: string[];
}

const SENTINEL_NOVA = '__nova__';

function CreateFields({ form, categorias }: CreateFieldsProps) {
  const tipo: 'FABRICADO' | 'REVENDA' = form.watch('tipo');
  const insumosQuery = useQuery({
    queryKey: ['insumos-ativos-revenda'],
    queryFn: () => listarInsumos({ ativo: true }),
    enabled: tipo === 'REVENDA',
  });

  return (
    <>
      <div className="space-y-2">
        <Label>Tipo</Label>
        <div className="flex gap-2">
          <label className="flex flex-1 items-center gap-2 rounded-md border p-3 cursor-pointer hover:bg-accent">
            <input
              type="radio"
              value="FABRICADO"
              checked={tipo === 'FABRICADO'}
              onChange={() => form.setValue('tipo', 'FABRICADO', { shouldValidate: true })}
            />
            <span>
              <strong>Fabricado</strong>
              <p className="text-xs text-muted-foreground">Usa ficha técnica (pizza, lasanha)</p>
            </span>
          </label>
          <label className="flex flex-1 items-center gap-2 rounded-md border p-3 cursor-pointer hover:bg-accent">
            <input
              type="radio"
              value="REVENDA"
              checked={tipo === 'REVENDA'}
              onChange={() => form.setValue('tipo', 'REVENDA', { shouldValidate: true })}
            />
            <span>
              <strong>Revenda</strong>
              <p className="text-xs text-muted-foreground">Vincula a 1 insumo (Coca, sorvete)</p>
            </span>
          </label>
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="codigo">Código</Label>
        <Input
          id="codigo"
          aria-invalid={Boolean(form.formState.errors.codigo)}
          {...form.register('codigo')}
        />
        {form.formState.errors.codigo && (
          <p className="text-sm text-destructive">
            {form.formState.errors.codigo.message as string}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="nome">Nome</Label>
        <Input
          id="nome"
          aria-invalid={Boolean(form.formState.errors.nome)}
          {...form.register('nome')}
        />
        {form.formState.errors.nome && (
          <p className="text-sm text-destructive">
            {form.formState.errors.nome.message as string}
          </p>
        )}
      </div>

      <CategoriaField form={form} categorias={categorias} />

      {tipo === 'REVENDA' && (
        <div className="space-y-2">
          <Label htmlFor="insumoRevendaId">Insumo vinculado</Label>
          <Select
            value={form.watch('insumoRevendaId') || ''}
            onValueChange={(v) => form.setValue('insumoRevendaId', v, { shouldValidate: true })}
          >
            <SelectTrigger id="insumoRevendaId">
              <SelectValue placeholder="Selecione o insumo…" />
            </SelectTrigger>
            <SelectContent>
              {(insumosQuery.data ?? []).map((i) => (
                <SelectItem key={i.id} value={i.id}>
                  {i.codigo} — {i.nome}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {form.formState.errors.insumoRevendaId && (
            <p className="text-sm text-destructive">
              {form.formState.errors.insumoRevendaId.message as string}
            </p>
          )}
          <p className="text-xs text-muted-foreground">
            A venda baixa 1 unidade desse insumo do estoque a cada produto vendido.
          </p>
        </div>
      )}
    </>
  );
}

interface UpdateFieldsProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any;
  categorias: string[];
}

function UpdateFields({ form, categorias }: UpdateFieldsProps) {
  return (
    <>
      <div className="space-y-2">
        <Label htmlFor="nome">Nome</Label>
        <Input
          id="nome"
          aria-invalid={Boolean(form.formState.errors.nome)}
          {...form.register('nome')}
        />
        {form.formState.errors.nome && (
          <p className="text-sm text-destructive">
            {form.formState.errors.nome.message as string}
          </p>
        )}
      </div>
      <CategoriaField form={form} categorias={categorias} />
    </>
  );
}

interface CategoriaProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any;
  categorias: string[];
}

function CategoriaField({ form, categorias }: CategoriaProps) {
  const categoriaAtual: string = form.watch('categoria') ?? '';
  const [modoNova, setModoNova] = useState(false);
  const usandoNova = modoNova || (categoriaAtual !== '' && !categorias.includes(categoriaAtual));

  return (
    <div className="space-y-2">
      <Label htmlFor="categoria">Categoria</Label>
      {usandoNova ? (
        <div className="flex gap-2">
          <Input
            id="categoria"
            placeholder="Nova categoria (ex.: Pizzas, Bebidas)"
            aria-invalid={Boolean(form.formState.errors.categoria)}
            {...form.register('categoria')}
          />
          {categorias.length > 0 && (
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setModoNova(false);
                form.setValue('categoria', '', { shouldValidate: false });
              }}
            >
              Escolher existente
            </Button>
          )}
        </div>
      ) : (
        <Select
          value={categoriaAtual || ''}
          onValueChange={(v) => {
            if (v === SENTINEL_NOVA) {
              setModoNova(true);
              form.setValue('categoria', '', { shouldValidate: false });
            } else {
              form.setValue('categoria', v, { shouldValidate: true });
            }
          }}
        >
          <SelectTrigger id="categoria">
            <SelectValue
              placeholder={categorias.length === 0 ? 'Cadastre a primeira…' : 'Selecione…'}
            />
          </SelectTrigger>
          <SelectContent>
            {categorias.map((c) => (
              <SelectItem key={c} value={c}>
                {c}
              </SelectItem>
            ))}
            <SelectItem value={SENTINEL_NOVA}>+ Nova categoria…</SelectItem>
          </SelectContent>
        </Select>
      )}
      {form.formState.errors.categoria && (
        <p className="text-sm text-destructive">
          {form.formState.errors.categoria.message as string}
        </p>
      )}
    </div>
  );
}
