# Tanda 3 cerrada y Tanda 4 avanzada

## Resumen

Se cerró una primera versión robusta del importador Word/DOCX y se avanzó el workspace documental con SideDock.

## Implementado

- DOCX en orden real del cuerpo (`w:body`).
- Estilos desde `word/styles.xml` cuando existen.
- Título desde `docProps/core.xml` cuando existe.
- Detección de títulos, subtítulos, párrafos, listas, tablas e imágenes.
- Diagnóstico con `DocumentImportReport`.
- `document/document.json` con metadata e import report.
- Document Workspace con módulos de SideDock: Estructura, Propiedades, Diagnóstico y Ayuda.

## Pendiente

- Extraer paneles del SideDock a clases independientes.
- Scroll sincronizado desde estructura al bloque central.
- Filtros por tipo de bloque.
- Guardarraíles de no-canvas y no-doble-scroll.
- Preparar Tanda 5 Reading Profile.
