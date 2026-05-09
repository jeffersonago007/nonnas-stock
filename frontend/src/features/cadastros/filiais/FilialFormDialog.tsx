import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
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
  type Filial,
  atualizarFilial,
  criarFilial,
  listarEmpresas,
} from './api';

const cnpjRegex = /^\d{14}$/;

const createSchema = z.object({
  empresaId: z.string().uuid('Selecione uma empresa'),
  nome: z.string().min(1, 'Nome é obrigatório').max(255),
  cnpj: z.string().regex(cnpjRegex, 'Informe 14 dígitos sem pontuação'),
  endereco: z.string().optional(),
});

const updateSchema = z.object({
  nome: z.string().min(1, 'Nome é obrigatório').max(255),
  endereco: z.string().optional(),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  filial: Filial | null;
}

export function FilialFormDialog({ open, onOpenChange, filial }: Props) {
  const isEdit = filial !== null;
  const queryClient = useQueryClient();

  const empresasQuery = useQuery({
    queryKey: ['empresas'],
    queryFn: listarEmpresas,
    enabled: open && !isEdit,
  });

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { empresaId: '', nome: '', cnpj: '', endereco: '' },
  });

  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { nome: '', endereco: '' },
  });

  useEffect(() => {
    if (open && filial) {
      updateForm.reset({ nome: filial.nome, endereco: filial.endereco ?? '' });
    } else if (open && !filial) {
      createForm.reset({ empresaId: '', nome: '', cnpj: '', endereco: '' });
    }
  }, [open, filial, createForm, updateForm]);

  const createMutation = useMutation({
    mutationFn: criarFilial,
    onSuccess: () => {
      toast.success('Filial criada');
      queryClient.invalidateQueries({ queryKey: ['filiais'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar a filial', error),
  });

  const updateMutation = useMutation({
    mutationFn: (values: UpdateValues) => atualizarFilial(filial!.id, values),
    onSuccess: () => {
      toast.success('Filial atualizada');
      queryClient.invalidateQueries({ queryKey: ['filiais'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar a filial', error),
  });

  const onSubmitCreate = (values: CreateValues) => {
    createMutation.mutate({
      empresaId: values.empresaId,
      nome: values.nome,
      cnpj: values.cnpj,
      endereco: values.endereco?.trim() || undefined,
    });
  };

  const onSubmitUpdate = (values: UpdateValues) => {
    updateMutation.mutate({
      nome: values.nome,
      endereco: values.endereco?.trim() || undefined,
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar filial' : 'Nova filial'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Atualize o nome e endereço. CNPJ não pode ser alterado.'
              : 'Cadastre uma nova filial vinculada a uma empresa existente.'}
          </DialogDescription>
        </DialogHeader>

        {isEdit ? (
          <form className="space-y-4" onSubmit={updateForm.handleSubmit(onSubmitUpdate)} noValidate>
            <FieldNome
              register={updateForm.register('nome')}
              error={updateForm.formState.errors.nome?.message}
            />
            <FieldEndereco
              register={updateForm.register('endereco')}
              error={updateForm.formState.errors.endereco?.message}
            />
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
          <form className="space-y-4" onSubmit={createForm.handleSubmit(onSubmitCreate)} noValidate>
            <div className="space-y-2">
              <Label htmlFor="empresaId">Empresa</Label>
              <Select
                value={createForm.watch('empresaId')}
                onValueChange={(v) => createForm.setValue('empresaId', v, { shouldValidate: true })}
              >
                <SelectTrigger id="empresaId">
                  <SelectValue
                    placeholder={
                      empresasQuery.isLoading ? 'Carregando empresas…' : 'Selecione uma empresa'
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {empresasQuery.data?.map((e) => (
                    <SelectItem key={e.id} value={e.id}>
                      {e.razaoSocial}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {createForm.formState.errors.empresaId && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.empresaId.message}
                </p>
              )}
            </div>

            <FieldNome
              register={createForm.register('nome')}
              error={createForm.formState.errors.nome?.message}
            />

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

            <FieldEndereco
              register={createForm.register('endereco')}
              error={createForm.formState.errors.endereco?.message}
            />

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

interface FieldProps {
  register: ReturnType<ReturnType<typeof useForm>['register']>;
  error?: string;
}

function FieldNome({ register, error }: FieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor="nome">Nome</Label>
      <Input id="nome" aria-invalid={Boolean(error)} {...register} />
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

function FieldEndereco({ register, error }: FieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor="endereco">Endereço (opcional)</Label>
      <Textarea id="endereco" rows={2} {...register} />
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
