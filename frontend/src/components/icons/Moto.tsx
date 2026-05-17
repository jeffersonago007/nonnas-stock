import { forwardRef } from 'react';
import type { LucideProps } from 'lucide-react';

export const Moto = forwardRef<SVGSVGElement, LucideProps>(
  ({ size = 24, color = 'currentColor', strokeWidth = 2, ...rest }, ref) => (
    <svg
      ref={ref}
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke={color}
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...rest}
    >
      <circle cx="5" cy="17" r="3" />
      <circle cx="19" cy="17" r="3" />
      <path d="M4 12 7 13 13 9 18 9 21 13" />
      <path d="M17 9 18 7" />
      <path d="M8 17h8" />
    </svg>
  ),
);
Moto.displayName = 'Moto';
