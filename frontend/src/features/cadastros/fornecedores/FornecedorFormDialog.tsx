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

import { type Fornecedor, atualizarFornecedor, criarFornecedor } from './api';

const cnpjRegex = /^\d{14}$/;

const createSchema = z.object({
  razaoSocial: z.string().min(1, 'Razão social é obrigatória'),
  cnpj: z.string().regex(cnpjRegex, 'Informe 14 dígitos sem pontuação'),
});
const updateSchema = z.object({
  razaoSocial: z.string().min(1, 'Razão social é obrigatória'),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  fornecedor: Fornecedor | null;
}

export function FornecedorFormDialog({ open, onOpenChange, fornecedor }: Props) {
  const isEdit = fornecedor !== null;
  const queryClient = useQueryClient();

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { razaoSocial: '', cnpj: '' },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { razaoSocial: '' },
  });

  useEffect(() => {
    if (open && fornecedor) {
      updateForm.reset({ razaoSocial: fornecedor.razaoSocial });
    } else if (open && !fornecedor) {
      createForm.reset();
    }
  }, [open, fornecedor, createForm, updateForm]);

  const createMutation = useMutation({
    mutationFn: criarFornecedor,
    onSuccess: () => {
      toast.success('Fornecedor criado');
      queryClient.invalidateQueries({ queryKey: ['fornecedores'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar o fornecedor', error),
  });
  const updateMutation = useMutation({
    mutationFn: (values: UpdateValues) => atualizarFornecedor(fornecedor!.id, values),
    onSuccess: () => {
      toast.success('Fornecedor atualizado');
      queryClient.invalidateQueries({ queryKey: ['fornecedores'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar', error),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar fornecedor' : 'Novo fornecedor'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Apenas a razão social pode ser editada. CNPJ é imutável após cadastro.'
              : 'Cadastre um novo fornecedor pelo CNPJ.'}
          </DialogDescription>
        </DialogHeader>

        {isEdit ? (
          <form
            className="space-y-4"
            onSubmit={updateForm.handleSubmit((v) => updateMutation.mutate(v))}
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="razaoSocial">Razão social</Label>
              <Input
                id="razaoSocial"
                aria-invalid={Boolean(updateForm.formState.errors.razaoSocial)}
                {...updateForm.register('razaoSocial')}
              />
              {updateForm.formState.errors.razaoSocial && (
                <p className="text-sm text-destructive">
                  {updateForm.formState.errors.razaoSocial.message}
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
            <div className="space-y-2">
              <Label htmlFor="razaoSocial">Razão social</Label>
              <Input
                id="razaoSocial"
                aria-invalid={Boolean(createForm.formState.errors.razaoSocial)}
                {...createForm.register('razaoSocial')}
              />
              {createForm.formState.errors.razaoSocial && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.razaoSocial.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="cnpj">CNPJ</Label>
              <Input
                id="cnpj"
                placeholder="14 dígitos sem pontuação"
                maxLength={14}
                aria-invalid={Boolean(createForm.formState.errors.cnpj)}
                {...createForm.register('cnpj')}
              />
              {createForm.formState.errors.cnpj && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.cnpj.message}
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
