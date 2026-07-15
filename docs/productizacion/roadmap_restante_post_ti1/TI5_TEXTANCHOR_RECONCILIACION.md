# TI5 — TextAnchor fuerte y reconciliación

## Objetivo

TI5 convierte `TextAnchor` en un contrato operativo para conservar asignaciones del proyecto cuando el documento fuente se refresca o cambia. La fuente sigue siendo solo lectura: DocuPodcast no edita el Word/DOCX/PDF/TXT/Markdown original.

## Regla central

Una capa narrativa no debe depender solamente de offsets frágiles. El proyecto debe conservar:

- rango documental (`DocumentTextRange`);
- texto seleccionado;
- `selectedTextHash`;
- contexto anterior y posterior;
- hash de snapshot fuente;
- estado de reconciliación.

## Estados

- `CURRENT`: el texto seleccionado sigue en el mismo rango.
- `RELOCATED`: el texto seleccionado se movió y se encontró una única coincidencia segura.
- `NEEDS_REVIEW`: hay ambigüedad o el anchor es legacy/pobre.
- `ORPHANED`: el texto seleccionado ya no aparece en el documento refrescado.

## Implementación

Se agrega:

- `TextAnchor.fromDocumentSelection(...)` para crear anchors ricos desde el documento importado.
- `TextAnchor.current(...)`, `relocated(...)`, `needsReview()` y `orphaned(...)`.
- `ReconcileTextAnchorsUseCase`.
- `TextAnchorReconciliationEntry`.
- `TextAnchorReconciliationReport`.
- `DocumentApplicationServices.reconcileTextAnchors`.

## Alcance deliberado

TI5 no usa OCR, IA, búsquedas semánticas ni edita documentos. Es un reconciliador determinista local:

1. confirma mismo rango por hash;
2. busca coincidencia exacta única;
3. marca revisión si hay múltiples coincidencias;
4. marca huérfano si desaparece.

## Relación con bloques visuales no narrables

Los bloques visuales (`IMAGE_NOTICE`, `TABLE_NOTICE`, `MATH_NOTICE`) pueden participar en una asignación de proyecto, pero su reconciliación visual avanzada sigue siendo posterior. TI5 fortalece el anclaje de texto y prepara la base para que futuras capas documentales no dependan de `ScriptTextRange` solamente.

## Tests

- `TextAnchorReconciliationUseCaseTest` valida CURRENT, RELOCATED y ORPHANED.
- `TextAnchorReconciliationTi5SourceTest` protege documentación y cableado de servicios.
