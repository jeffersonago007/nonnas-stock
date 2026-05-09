import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';

import { Button } from './button';

describe('Button', () => {
  it('renderiza variant default com classe bg-primary (mapeada à brand.red via CSS var)', () => {
    render(<Button>Entrar</Button>);

    const button = screen.getByRole('button', { name: /entrar/i });
    expect(button).toBeInTheDocument();
    expect(button.className).toContain('bg-primary');
  });

  it('aplica variant destructive quando solicitado', () => {
    render(<Button variant="destructive">Excluir</Button>);

    const button = screen.getByRole('button', { name: /excluir/i });
    expect(button.className).toContain('bg-destructive');
  });
});
