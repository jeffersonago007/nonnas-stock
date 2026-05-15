import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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
import { listarFiliais } from '@/features/cadastros/filiais/api';

import { type Usuario, atualizarUsuario, criarUsuario } from './api';

const SEM_FILIAL = '__sem_filial__';

const perfilEnum = z.enum(['ADMIN', 'GERENTE', 'OPERADOR', 'CONSULTA']);

const senhaPolicy = z
  .string()
  .min(10, 'Mínimo 10 caracteres')
  .regex(/[A-Za-z]/, 'Precisa ao menos 1 letra')
  .regex(/\d/, 'Precisa ao menos 1 número')
  .regex(/[^A-Za-z0-9]/, 'Precisa ao menos 1 caractere especial');

const createSchema = z
  .object({
    nome: z.string().min(1, 'Nome é obrigatório').max(255),
    email: z.string().email('Informe um e-mail válido'),
    senha: senhaPolicy,
    perfil: perfilEnum,
    filialId: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    // Apenas ADMIN pode ficar sem filial. Demais perfis exigem vínculo —
    // a escopagem de dados é amarrada nesse campo (T-RBAC-01).
    if (data.perfil !== 'ADMIN' && (!data.filialId || data.filialId === SEM_FILIAL)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['filialId'],
        message: 'Filial é obrigatória para perfis diferentes de Administrador',
      });
    }
  });
const updateSchema = z.object({
  nome: z.string().min(1, 'Nome é obrigatório').max(255),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  usuario: Usuario | null;
}

export function UsuarioFormDialog({ open, onOpenChange, usuario }: Props) {
  const isEdit = usuario !== null;
  const queryClient = useQueryClient();

  const filiaisQuery = useQuery({
    queryKey: ['filiais-para-usuario'],
    queryFn: () => listarFiliais(),
    enabled: open && !isEdit,
  });

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      nome: '',
      email: '',
      senha: '',
      perfil: 'OPERADOR',
      filialId: SEM_FILIAL,
    },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { nome: '' },
  });

  useEffect(() => {
    if (open && usuario) {
      updateForm.reset({ nome: usuario.nome });
    } else if (open && !usuario) {
      createForm.reset({
        nome: '',
        email: '',
        senha: '',
        perfil: 'OPERADOR',
        filialId: SEM_FILIAL,
      });
    }
  }, [open, usuario, createForm, updateForm]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-usuarios'] });
  };

  const createMutation = useMutation({
    mutationFn: (values: CreateValues) =>
      criarUsuario({
        nome: values.nome,
        email: values.email,
        senha: values.senha,
        perfil: values.perfil,
        filialId: values.filialId === SEM_FILIAL ? null : values.filialId,
      }),
    onSuccess: () => {
      toast.success('Usuário criado');
      invalidate();
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar o usuário', error),
  });
  const updateMutation = useMutation({
    mutationFn: (values: UpdateValues) => atualizarUsuario(usuario!.id, values),
    onSuccess: () => {
      toast.success('Usuário atualizado');
      invalidate();
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar', error),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar usuário' : 'Novo usuário'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Apenas o nome pode ser editado. E-mail e senha são fluxos separados.'
              : 'Cadastre um novo usuário do sistema.'}
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
            <div className="space-y-2">
              <Label htmlFor="email">E-mail</Label>
              <Input
                id="email"
                type="email"
                aria-invalid={Boolean(createForm.formState.errors.email)}
                {...createForm.register('email')}
              />
              {createForm.formState.errors.email && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.email.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="senha">Senha</Label>
              <Input
                id="senha"
                type="password"
                placeholder="Mín 10 chars, 1 letra, 1 número, 1 especial"
                aria-invalid={Boolean(createForm.formState.errors.senha)}
                {...createForm.register('senha')}
              />
              {createForm.formState.errors.senha && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.senha.message}
                </p>
              )}
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="perfil">Perfil</Label>
                <Select
                  value={createForm.watch('perfil')}
                  onValueChange={(v) =>
                    createForm.setValue('perfil', v as CreateValues['perfil'], {
                      shouldValidate: true,
                    })
                  }
                >
                  <SelectTrigger id="perfil">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ADMIN">Administrador</SelectItem>
                    <SelectItem value="GERENTE">Gerente</SelectItem>
                    <SelectItem value="OPERADOR">Operador</SelectItem>
                    <SelectItem value="CONSULTA">Consulta</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="filialId">
                  Filial{createForm.watch('perfil') !== 'ADMIN' && ' *'}
                </Label>
                <Select
                  value={createForm.watch('filialId') ?? SEM_FILIAL}
                  onValueChange={(v) =>
                    createForm.setValue('filialId', v, { shouldValidate: true })
                  }
                >
                  <SelectTrigger
                    id="filialId"
                    aria-invalid={Boolean(createForm.formState.errors.filialId)}
                  >
                    <SelectValue
                      placeholder={filiaisQuery.isLoading ? 'Carregando…' : 'Selecione…'}
                    />
                  </SelectTrigger>
                  <SelectContent>
                    {createForm.watch('perfil') === 'ADMIN' && (
                      <SelectItem value={SEM_FILIAL}>Sem filial (acesso global)</SelectItem>
                    )}
                    {filiaisQuery.data?.map((f) => (
                      <SelectItem key={f.id} value={f.id}>
                        {f.nome}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {createForm.formState.errors.filialId && (
                  <p className="text-sm text-destructive">
                    {createForm.formState.errors.filialId.message}
                  </p>
                )}
              </div>
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
