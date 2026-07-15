# Tanda 04 avance — Memoria de workspace documental

## Estado

Tanda 4 queda avanzada con un workspace documental más serio. Ya no es una lista plana: ahora tiene zona lateral de estructura, diagnóstico de importación, métricas y selección de bloques.

## Implementado

- `DocumentWorkspaceView` usa `SplitPane`.
- Zona lateral tipo SideDock inicial:
  - sección `Estructura` con `ListView<DocumentBlock>`;
  - sección `Diagnóstico` con `ListView<DocumentImportDiagnostic>`.
- Centro documental con:
  - título;
  - resumen;
  - métricas visuales;
  - panel de bloque seleccionado;
  - tarjetas por bloque.
- Selección desde estructura lateral.
- Clic en diagnóstico con `blockId` enfoca el bloque correspondiente.
- Preparación conceptual para futuro inspector de bloque, perfil de lectura y generación de guion.

## CSS agregado

- métricas de documento;
- panel seleccionado;
- listas de estructura y diagnóstico;
- estados info/warning/error;
- clases para bloque seleccionado y elementos estructurales.

## Lo que falta para cerrar completamente Tanda 4

- SideDock modular reutilizable estilo DMS.
- Panel de propiedades de bloque más formal.
- Filtros por tipo de bloque.
- Acciones directas sobre bloque: marcar como título, subtítulo, párrafo, ignorar.
- Scroll/foco visual fino hacia la tarjeta seleccionada.
- Persistencia del estado de selección/scroll en `view`.

## Criterio de producto

El workspace Documento debe permitir revisar el Word importado antes de convertirlo en guion. Aquí el usuario debe poder detectar si sus títulos/subtítulos/imágenes fueron reconocidos correctamente.
