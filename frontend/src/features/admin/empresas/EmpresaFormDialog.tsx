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

import { type Empresa, atualizarEmpresa, criarEmpresa } from './api';

const cnpjRegex = /^\d{14}$/;

const createSchema = z.object({
  razaoSocial: z.string().min(1, 'Razão social é obrigatória').max(255),
  cnpj: z.string().regex(cnpjRegex, 'Informe 14 dígitos sem pontuação'),
});
const updateSchema = z.object({
  razaoSocial: z.string().min(1, 'Razão social é obrigatória').max(255),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  empresa: Empresa | null;
}

export function EmpresaFormDialog({ open, onOpenChange, empresa }: Props) {
  const isEdit = empresa !== null;
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
    if (open && empresa) {
      updateForm.reset({ razaoSocial: empresa.razaoSocial });
    } else if (open && !empresa) {
      createForm.reset({ razaoSocial: '', cnpj: '' });
    }
  }, [open, empresa, createForm, updateForm]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-empresas'] });
    queryClient.invalidateQueries({ queryKey: ['empresas'] });
  };

  const createMutation = useMutation({
    mutationFn: criarEmpresa,
    onSuccess: () => {
      toast.success('Empresa criada');
      invalidate();
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar a empresa', error),
  });
  const updateMutation = useMutation({
    mutationFn: (values: UpdateValues) => atualizarEmpresa(empresa!.id, values),
    onSuccess: () => {
      toast.success('Empresa atualizada');
      invalidate();
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar', error),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar empresa' : 'Nova empresa'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Apenas a razão social pode ser editada. CNPJ é imutável após cadastro.'
              : 'Cadastre uma nova empresa do grupo.'}
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
