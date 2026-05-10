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

import { type Categoria, atualizarCategoria, criarCategoria } from './api';

const schema = z.object({
  nome: z.string().min(1, 'Nome é obrigatório').max(255),
});

type Values = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  categoria: Categoria | null;
}

export function CategoriaFormDialog({ open, onOpenChange, categoria }: Props) {
  const isEdit = categoria !== null;
  const queryClient = useQueryClient();

  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { nome: '' },
  });

  useEffect(() => {
    if (open) {
      form.reset({ nome: categoria?.nome ?? '' });
    }
  }, [open, categoria, form]);

  const createMutation = useMutation({
    mutationFn: (values: Values) => criarCategoria({ nome: values.nome }),
    onSuccess: () => {
      toast.success('Categoria criada');
      queryClient.invalidateQueries({ queryKey: ['admin-categorias'] });
      queryClient.invalidateQueries({ queryKey: ['categorias-insumo'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar a categoria', error),
  });

  const updateMutation = useMutation({
    mutationFn: (values: Values) => atualizarCategoria(categoria!.id, { nome: values.nome }),
    onSuccess: () => {
      toast.success('Categoria atualizada');
      queryClient.invalidateQueries({ queryKey: ['admin-categorias'] });
      queryClient.invalidateQueries({ queryKey: ['categorias-insumo'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar', error),
  });

  const submit = (values: Values) =>
    isEdit ? updateMutation.mutate(values) : createMutation.mutate(values);
  const pending = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar categoria' : 'Nova categoria'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Apenas o nome pode ser editado. A categoria pai é imutável após cadastro.'
              : 'Cadastre uma nova categoria de insumo.'}
          </DialogDescription>
        </DialogHeader>

        <form className="space-y-4" onSubmit={form.handleSubmit(submit)} noValidate>
          <div className="space-y-2">
            <Label htmlFor="nome">Nome</Label>
            <Input
              id="nome"
              autoFocus
              aria-invalid={Boolean(form.formState.errors.nome)}
              {...form.register('nome')}
            />
            {form.formState.errors.nome && (
              <p className="text-sm text-destructive">{form.formState.errors.nome.message}</p>
            )}
          </div>
          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={pending}>
              {pending ? (isEdit ? 'Salvando…' : 'Criando…') : isEdit ? 'Salvar' : 'Criar'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
