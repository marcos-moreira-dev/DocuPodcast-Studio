# Tanda 69B — Hotfix de source tests del frente honesto

## Motivo

La Tanda 69 introdujo cambios deliberados de lenguaje visible para evitar sobrepromesas:

- `guion` deja de aparecer como objeto padre y pasa a `narración` o proyección avanzada;
- `Abrir Word/DOCX` se recontrata como `Abrir documento`;
- PDF no se ofrece en el frente hasta tener importador real;
- Configuración pasa a ser editable y persistente.

La validación local mostró que el código compilaba, pero varios source tests seguían anclados a cadenas antiguas del frente. Esta tanda corrige esos guardarraíles para que protejan el contrato actual sin obligar a regresar a labels previos.

## Corrección

Se actualizan tests fuente de Documento, playback, importación Markdown, exportación, Configuración, video y productización para aceptar el vocabulario actual:

- `Exportar narración Markdown` en lugar de exigir solo `Exportar guion Markdown`.
- `Importar narración Markdown` en lugar de exigir solo `Importar guion Markdown`.
- `Fragmentos listos antes de empezar` y `Fragmentos adelantados` como labels editables de buffer.
- Configuración editable y persistente como bodega técnica separada.
- `Lector narrado local para Word/DOCX, Markdown y TXT` como frente honesto.

## Alcance

No cambia comportamiento productivo. No avanza a storyboard/video ni al cierre RC. Esta tanda solo deja verde la base después del cambio de vocabulario y prepara la discusión seria del frontend.

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Criterio: todos los tests deben pasar.
