# Memoria — avance Tanda 3

Se adelantó el importador DOCX y la materialización del documento importado.

## Cadena ya existente

DOCX externo → `ReadableDocument` → `DocumentWorkspaceView` → guardar proyecto → `source/` + `document/document.json` + assets relativos.

## Lo que falta para cerrar Tanda 3 completa

- Lectura más rica de listas y tablas.
- Alt text más exhaustivo de imágenes.
- Diagnóstico de importación.
- Tests con DOCX más variados.
- Posible migración a Apache POI si el extractor JDK resulta insuficiente.
