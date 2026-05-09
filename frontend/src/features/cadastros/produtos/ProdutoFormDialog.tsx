import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { toastError } from '@/lib/toastError';

import { type Produto, atualizarProduto, criarProduto } from './api';

const createSchema = z.object({
  codigo: z.string().min(1, 'Código é obrigatório'),
  nome: z.string().min(1, 'Nome é obrigatório'),
  categoria: z.string().min(1, 'Categoria é obrigatória'),
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

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { codigo: '', nome: '', categoria: '' },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { nome: '', categoria: '' },
  });

  useEffect(() => {
    if (open && produto) {
      updateForm.reset({ nome: produto.nome, categoria: produto.categoria });
    } else if (open && !produto) {
      createForm.reset();
    }
  }, [open, produto, createForm, updateForm]);

  const createMutation = useMutation({
    mutationFn: criarProduto,
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
              ? 'Código não pode ser alterado.'
              : 'Cadastre um produto que será vendido pela rede.'}
          </DialogDescription>
        </DialogHeader>

        {isEdit ? (
          <form
            className="space-y-4"
            onSubmit={updateForm.handleSubmit((v) => updateMutation.mutate(v))}
            noValidate
          >
            <FormFields form={updateForm} hideCodigo />
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
            <FormFields form={createForm} />
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

interface FieldsProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any;
  hideCodigo?: boolean;
}

function FormFields({ form, hideCodigo = false }: FieldsProps) {
  return (
    <>
      {!hideCodigo && (
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
      )}
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
      <div className="space-y-2">
        <Label htmlFor="categoria">Categoria</Label>
        <Input
          id="categoria"
          placeholder="Ex.: Pizzas, Bebidas, Sobremesas"
          aria-invalid={Boolean(form.formState.errors.categoria)}
          {...form.register('categoria')}
        />
        {form.formState.errors.categoria && (
          <p className="text-sm text-destructive">
            {form.formState.errors.categoria.message as string}
          </p>
        )}
      </div>
    </>
  );
}
