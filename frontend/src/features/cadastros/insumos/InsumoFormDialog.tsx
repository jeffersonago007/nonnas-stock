import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { toastError } from '@/lib/toastError';

import {
  type Insumo,
  atualizarInsumo,
  criarInsumo,
  listarCategorias,
  listarUnidades,
} from './api';

const createSchema = z.object({
  codigo: z.string().min(1, 'Código é obrigatório').max(50),
  nome: z.string().min(1, 'Nome é obrigatório').max(255),
  categoriaId: z.string().uuid('Selecione uma categoria'),
  unidadeBaseId: z.string().uuid('Selecione a unidade base'),
  controlaLote: z.boolean(),
  controlaValidade: z.boolean(),
});

const updateSchema = z.object({
  nome: z.string().min(1, 'Nome é obrigatório').max(255),
  categoriaId: z.string().uuid('Selecione uma categoria'),
  controlaLote: z.boolean(),
  controlaValidade: z.boolean(),
  // string vinda do <Input type="number"> — conversão para number na submissão
  diasAlertaVencimento: z
    .string()
    .optional()
    .refine((v) => !v || /^\d+$/.test(v), 'Informe um número inteiro')
    .refine((v) => !v || (Number(v) >= 1 && Number(v) <= 90), 'Entre 1 e 90 dias'),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  insumo: Insumo | null;
}

export function InsumoFormDialog({ open, onOpenChange, insumo }: Props) {
  const isEdit = insumo !== null;
  const queryClient = useQueryClient();

  const categoriasQuery = useQuery({
    queryKey: ['categorias-insumo'],
    queryFn: listarCategorias,
    enabled: open,
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
    enabled: open && !isEdit,
  });

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      codigo: '',
      nome: '',
      categoriaId: '',
      unidadeBaseId: '',
      controlaLote: true,
      controlaValidade: true,
    },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: {
      nome: '',
      categoriaId: '',
      controlaLote: true,
      controlaValidade: true,
      diasAlertaVencimento: '',
    },
  });

  useEffect(() => {
    if (open && insumo) {
      updateForm.reset({
        nome: insumo.nome,
        categoriaId: insumo.categoriaId,
        controlaLote: insumo.controlaLote,
        controlaValidade: insumo.controlaValidade,
        diasAlertaVencimento:
          insumo.diasAlertaVencimento != null ? String(insumo.diasAlertaVencimento) : '',
      });
    } else if (open && !insumo) {
      createForm.reset();
    }
  }, [open, insumo, createForm, updateForm]);

  const createMutation = useMutation({
    mutationFn: criarInsumo,
    onSuccess: () => {
      toast.success('Produto criado');
      queryClient.invalidateQueries({ queryKey: ['insumos'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar o produto', error),
  });

  const updateMutation = useMutation({
    mutationFn: (values: UpdateValues) =>
      atualizarInsumo(insumo!.id, {
        nome: values.nome,
        categoriaId: values.categoriaId,
        controlaLote: values.controlaLote,
        controlaValidade: values.controlaValidade,
        // só envia se a flag está ativa e o usuário preencheu — payload null
        // não toca no campo no backend (decisão consciente do MVP).
        diasAlertaVencimento:
          values.controlaValidade && values.diasAlertaVencimento
            ? Number(values.diasAlertaVencimento)
            : undefined,
      }),
    onSuccess: () => {
      toast.success('Produto atualizado');
      queryClient.invalidateQueries({ queryKey: ['insumos'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar o produto', error),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar produto' : 'Novo produto'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Você pode editar nome, categoria e controles de lote/validade. Código e unidade-base permanecem imutáveis (afetam histórico de movimentações).'
              : 'Cadastre um novo produto com unidade-base e controles de lote/validade.'}
          </DialogDescription>
        </DialogHeader>

        {isEdit ? (
          <form className="space-y-4" onSubmit={updateForm.handleSubmit((v) => updateMutation.mutate(v))} noValidate>
            <div className="space-y-2">
              <Label htmlFor="nome">Nome</Label>
              <Input id="nome" aria-invalid={Boolean(updateForm.formState.errors.nome)} {...updateForm.register('nome')} />
              {updateForm.formState.errors.nome && (
                <p className="text-sm text-destructive">{updateForm.formState.errors.nome.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-categoriaId">Categoria</Label>
              <Select
                value={updateForm.watch('categoriaId')}
                onValueChange={(v) => updateForm.setValue('categoriaId', v, { shouldValidate: true })}
              >
                <SelectTrigger id="edit-categoriaId">
                  <SelectValue placeholder={categoriasQuery.isLoading ? 'Carregando…' : 'Selecione…'} />
                </SelectTrigger>
                <SelectContent>
                  {categoriasQuery.data?.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {updateForm.formState.errors.categoriaId && (
                <p className="text-sm text-destructive">{updateForm.formState.errors.categoriaId.message}</p>
              )}
            </div>
            <div className="flex flex-col gap-3">
              <ToggleRow
                id="edit-controlaLote"
                label="Controla lote"
                description="Movimentações terão lote obrigatório"
                checked={updateForm.watch('controlaLote')}
                onCheckedChange={(v) => updateForm.setValue('controlaLote', v)}
              />
              <ToggleRow
                id="edit-controlaValidade"
                label="Controla validade"
                description="Lotes terão data de validade obrigatória"
                checked={updateForm.watch('controlaValidade')}
                onCheckedChange={(v) => updateForm.setValue('controlaValidade', v)}
              />
            </div>
            {updateForm.watch('controlaValidade') && (
              <div className="space-y-2">
                <Label htmlFor="edit-diasAlertaVencimento">
                  Dias antes do vencimento para alertar (opcional)
                </Label>
                <Input
                  id="edit-diasAlertaVencimento"
                  type="number"
                  min={1}
                  max={90}
                  step={1}
                  placeholder="Ex.: 7"
                  aria-invalid={Boolean(updateForm.formState.errors.diasAlertaVencimento)}
                  {...updateForm.register('diasAlertaVencimento')}
                />
                {updateForm.formState.errors.diasAlertaVencimento && (
                  <p className="text-sm text-destructive">
                    {updateForm.formState.errors.diasAlertaVencimento.message}
                  </p>
                )}
                <p className="text-xs text-muted-foreground">
                  Vazio = usa o padrão global de alertas. Range aceito: 1 a 90 dias.
                </p>
              </div>
            )}
            <p className="text-xs text-muted-foreground">
              Atenção: alterar categoria ou controles afeta alertas e movimentações futuras.
              Lotes existentes não são removidos.
            </p>
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
            onSubmit={createForm.handleSubmit((values) => createMutation.mutate(values))}
            noValidate
          >
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="codigo">Código</Label>
                <Input id="codigo" {...createForm.register('codigo')} aria-invalid={Boolean(createForm.formState.errors.codigo)} />
                {createForm.formState.errors.codigo && (
                  <p className="text-sm text-destructive">{createForm.formState.errors.codigo.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="nome">Nome</Label>
                <Input id="nome" {...createForm.register('nome')} aria-invalid={Boolean(createForm.formState.errors.nome)} />
                {createForm.formState.errors.nome && (
                  <p className="text-sm text-destructive">{createForm.formState.errors.nome.message}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="categoriaId">Categoria</Label>
                <Select
                  value={createForm.watch('categoriaId')}
                  onValueChange={(v) => createForm.setValue('categoriaId', v, { shouldValidate: true })}
                >
                  <SelectTrigger id="categoriaId">
                    <SelectValue placeholder={categoriasQuery.isLoading ? 'Carregando…' : 'Selecione…'} />
                  </SelectTrigger>
                  <SelectContent>
                    {categoriasQuery.data?.map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.nome}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {createForm.formState.errors.categoriaId && (
                  <p className="text-sm text-destructive">{createForm.formState.errors.categoriaId.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="unidadeBaseId">Unidade base</Label>
                <Select
                  value={createForm.watch('unidadeBaseId')}
                  onValueChange={(v) => createForm.setValue('unidadeBaseId', v, { shouldValidate: true })}
                >
                  <SelectTrigger id="unidadeBaseId">
                    <SelectValue placeholder={unidadesQuery.isLoading ? 'Carregando…' : 'Selecione…'} />
                  </SelectTrigger>
                  <SelectContent>
                    {unidadesQuery.data?.map((u) => (
                      <SelectItem key={u.id} value={u.id}>
                        {u.codigo} — {u.nome}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {createForm.formState.errors.unidadeBaseId && (
                  <p className="text-sm text-destructive">{createForm.formState.errors.unidadeBaseId.message}</p>
                )}
              </div>
            </div>

            <div className="flex flex-col gap-3">
              <ToggleRow
                id="controlaLote"
                label="Controla lote"
                description="Movimentações terão lote obrigatório"
                checked={createForm.watch('controlaLote')}
                onCheckedChange={(v) => createForm.setValue('controlaLote', v)}
              />
              <ToggleRow
                id="controlaValidade"
                label="Controla validade"
                description="Lotes terão data de validade obrigatória"
                checked={createForm.watch('controlaValidade')}
                onCheckedChange={(v) => createForm.setValue('controlaValidade', v)}
              />
            </div>

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

interface ToggleRowProps {
  id: string;
  label: string;
  description: string;
  checked: boolean;
  onCheckedChange: (v: boolean) => void;
}

function ToggleRow({ id, label, description, checked, onCheckedChange }: ToggleRowProps) {
  return (
    <div className="flex items-center justify-between rounded-md border p-3">
      <div>
        <Label htmlFor={id} className="text-sm font-medium">
          {label}
        </Label>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
      <Switch id={id} checked={checked} onCheckedChange={onCheckedChange} />
    </div>
  );
}
