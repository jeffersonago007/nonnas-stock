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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { toastError } from '@/lib/toastError';

import { type Unidade, atualizarUnidade, criarUnidade } from './api';

const tipoEnum = z.enum(['PESO', 'VOLUME', 'UNIDADE']);

const createSchema = z.object({
  codigo: z.string().min(1, 'Código é obrigatório').max(20),
  nome: z.string().min(1, 'Nome é obrigatório').max(100),
  tipo: tipoEnum,
});
const updateSchema = z.object({
  nome: z.string().min(1, 'Nome é obrigatório').max(100),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  unidade: Unidade | null;
}

export function UnidadeFormDialog({ open, onOpenChange, unidade }: Props) {
  const isEdit = unidade !== null;
  const queryClient = useQueryClient();

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { codigo: '', nome: '', tipo: 'UNIDADE' },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { nome: '' },
  });

  useEffect(() => {
    if (open && unidade) {
      updateForm.reset({ nome: unidade.nome });
    } else if (open && !unidade) {
      createForm.reset({ codigo: '', nome: '', tipo: 'UNIDADE' });
    }
  }, [open, unidade, createForm, updateForm]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-unidades'] });
    queryClient.invalidateQueries({ queryKey: ['unidades-medida'] });
  };

  const createMutation = useMutation({
    mutationFn: criarUnidade,
    onSuccess: () => {
      toast.success('Unidade criada');
      invalidate();
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar a unidade', error),
  });
  const updateMutation = useMutation({
    mutationFn: (values: UpdateValues) => atualizarUnidade(unidade!.id, values),
    onSuccess: () => {
      toast.success('Unidade atualizada');
      invalidate();
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar', error),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar unidade' : 'Nova unidade'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Apenas o nome pode ser editado. Código e tipo são imutáveis após cadastro.'
              : 'Cadastre uma nova unidade de medida.'}
          </DialogDescription>
        </DialogHeader>

        {isEdit ? (
          <form
            className="space-y-4"
            onSubmit={updateForm.handleSubmit((v) => updateMutation.mutate(v))}
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="nome">Nome</Label>
              <Input
                id="nome"
                aria-invalid={Boolean(updateForm.formState.errors.nome)}
                {...updateForm.register('nome')}
              />
              {updateForm.formState.errors.nome && (
                <p className="text-sm text-destructive">
                  {updateForm.formState.errors.nome.message}
                </p>
              )}
            </div>
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
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="codigo">Código</Label>
                <Input
                  id="codigo"
                  placeholder="Ex.: KG, L, UN"
                  maxLength={20}
                  aria-invalid={Boolean(createForm.formState.errors.codigo)}
                  {...createForm.register('codigo')}
                />
                {createForm.formState.errors.codigo && (
                  <p className="text-sm text-destructive">
                    {createForm.formState.errors.codigo.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="tipo">Tipo</Label>
                <Select
                  value={createForm.watch('tipo')}
                  onValueChange={(v) =>
                    createForm.setValue('tipo', v as CreateValues['tipo'], {
                      shouldValidate: true,
                    })
                  }
                >
                  <SelectTrigger id="tipo">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PESO">Peso</SelectItem>
                    <SelectItem value="VOLUME">Volume</SelectItem>
                    <SelectItem value="UNIDADE">Unidade</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="nome">Nome</Label>
              <Input
                id="nome"
                aria-invalid={Boolean(createForm.formState.errors.nome)}
                {...createForm.register('nome')}
              />
              {createForm.formState.errors.nome && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.nome.message}
                </p>
              )}
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
