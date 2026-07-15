# Memoria — Tanda 5B Reading Profile

Esta tanda cerró el perfil de lectura como concepto de producto. Hasta la Tanda 5 existía un perfil académico Word-first fijo; desde la Tanda 5B, el perfil es parte del proyecto y se puede editar desde el workspace Documento.

## Decisiones cerradas

1. El perfil de lectura vive en `.docupodcast.json`, no solo en memoria.
2. El perfil activo se guarda también como referencia en `document/document.json` cuando se materializa el documento.
3. El usuario puede previsualizar el impacto antes de aplicar reglas.
4. Las reclasificaciones manuales de bloques se respetan y no son pisadas por el perfil.
5. DOCX sigue siendo la entrada primaria; el perfil existe para corregir cómo se interpretan notas Word reales.

## Clases agregadas o modificadas

- `DocuPodcastProject` ahora contiene `ReadingProfile`.
- `DocuPodcastProjectJsonReader` y `DocuPodcastProjectJsonWriter` leen/escriben `readingProfile`.
- `ReadableDocumentWorkspaceRepository` escribe el perfil en el snapshot documental.
- `PreviewReadingProfileUseCase` calcula cambios propuestos sin mutar.
- `DocumentReadingProfilePanel` ahora es editor real.
- `DocuPodcastShellViewModel` conserva `activeReadingProfile` y lo sincroniza con la sesión/proyecto.

## Camino habilitado

El siguiente artefacto natural es `NarrationScriptDocument`: tomar bloques narrables del documento ya clasificado y convertirlos en segmentos de guion.
