# Tanda 60B — Hotfix verde + contrato Refrescar contenido

## Base

Aplicable sobre Tanda 60.

## Motivo

La validación local de T60 mostró fallos en source tests documentales heredados de T58C/T59/T59C. Los fallos no corresponden a compilación productiva sino a tests demasiado acoplados a textos de tanda anterior.

Además, se incorpora una regla de producto: todos los documentos fuente son solo lectura, pero debe existir una acción **Refrescar contenido** para comparar cambios hechos fuera de DocuPodcast.

## Correcciones

- `DesignScaffoldingPrinciplesSourceTest` ya no exige que `VALIDATION.md` se quede en Tanda 59.
- `SmokeExploratorioMinimoSourceTest` conserva el protocolo T58C, pero acepta que la tanda vigente avance.
- `REFERENCIA_OFIMATICA_WORD_LIKE.md` explicita “inspiración de lectura sí” y “edición ofimática completa no”.
- `docs/85_TANDA_59C_CONTRATO_DOCUMENTOS_SOLO_LECTURA.md` explicita “fuente inmutable”.

## Nuevo contrato

Se agrega `CONTRATO_REFRESCAR_DOCUMENTO_FUENTE_V1.md`.

La acción **Refrescar contenido** debe:

- leer nuevamente el archivo fuente;
- comparar contra snapshot importado;
- mostrar impacto;
- marcar audio obsoleto si el texto cambió;
- conservar capas cuando pueda reconciliarse el texto;
- dejar vínculos dudosos en revisión;
- no sobrescribir la fuente original.

## Criterio de salida

La base debe volver a verde antes de iniciar refactor real de coordinadores.
