# Memoria — Tanda 2.5 implementada

Se agregó la primera integración visible entre proyecto, UI y Word/DOCX.

## Decisiones

- La UI puede crear, abrir, guardar y cerrar proyectos `.docupodcast.json`.
- El dirty state vive en `ProjectSession`, no en el dominio persistido.
- El cierre con cambios sin guardar requiere confirmación.
- Word/DOCX ya es accionable desde menú y toolbar.
- Se agregó un importador DOCX mínimo sin dependencias externas para validar el flujo Word-first.

## Próximo paso natural

Persistir `ReadableDocument` en `document/document.json` y copiar/referenciar el DOCX fuente como asset relativo del proyecto.
