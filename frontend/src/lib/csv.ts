/**
 * Exporta um array de objetos como CSV (UTF-8 com BOM, separador `;` —
 * Excel pt-BR abre direto). Strings com `;`, `"` ou quebra de linha são
 * escapadas com aspas duplas.
 *
 * `columns` define a ordem das colunas e o cabeçalho. O `value` é
 * livre — quem chama formata número/data/moeda como achar melhor antes
 * de mandar a string para o CSV.
 */
export interface CsvColumn<T> {
  header: string;
  value: (row: T) => string | number | null | undefined;
}

export function downloadCsv<T>(filename: string, columns: CsvColumn<T>[], rows: T[]): void {
  const escape = (v: string | number | null | undefined): string => {
    if (v === null || v === undefined) return '';
    const s = String(v);
    if (/[";\n\r]/.test(s)) {
      return '"' + s.replace(/"/g, '""') + '"';
    }
    return s;
  };

  const headerLine = columns.map((c) => escape(c.header)).join(';');
  const bodyLines = rows.map((row) =>
    columns.map((c) => escape(c.value(row))).join(';'),
  );
  // BOM (﻿) faz o Excel pt-BR detectar UTF-8 automaticamente.
  const content = '﻿' + [headerLine, ...bodyLines].join('\r\n');

  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename.endsWith('.csv') ? filename : `${filename}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
