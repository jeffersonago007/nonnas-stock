import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader } from '@/components/ui/card';
import { extractProblem } from '@/lib/api';
import { login } from './api';
import { useAuthStore } from './store';

const schema = z.object({
  email: z.string().email('Informe um e-mail válido'),
  senha: z.string().min(1, 'Senha é obrigatória'),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const [serverError, setServerError] = useState<string | null>(null);
  const setSession = useAuthStore((s) => s.setSession);
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', senha: '' },
  });

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setSession(data.accessToken, data.usuario);
      navigate('/dashboard', { replace: true });
    },
    onError: (error) => {
      const problem = extractProblem(error);
      setServerError(problem?.detail ?? 'Não foi possível autenticar. Tente novamente.');
    },
  });

  const onSubmit = (values: FormValues) => {
    setServerError(null);
    mutation.mutate(values);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-bg px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-3 text-center">
          <img
            src="/logo-nonnas.png"
            alt="Nonnas Paola — Churrascaria & Pizzaria"
            className="mx-auto h-32 w-auto object-contain"
          />
          <CardDescription>Acesse o painel de controle de estoque</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="space-y-2">
              <Label htmlFor="email">E-mail</Label>
              <Input
                id="email"
                type="email"
                autoComplete="username"
                aria-invalid={Boolean(errors.email)}
                {...register('email')}
              />
              {errors.email && (
                <p className="text-sm text-destructive">{errors.email.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="senha">Senha</Label>
              <Input
                id="senha"
                type="password"
                autoComplete="current-password"
                aria-invalid={Boolean(errors.senha)}
                {...register('senha')}
              />
              {errors.senha && (
                <p className="text-sm text-destructive">{errors.senha.message}</p>
              )}
            </div>

            {serverError && (
              <p role="alert" className="text-sm text-destructive">
                {serverError}
              </p>
            )}

            <Button type="submit" className="w-full" disabled={mutation.isPending}>
              {mutation.isPending ? 'Entrando…' : 'Entrar'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
