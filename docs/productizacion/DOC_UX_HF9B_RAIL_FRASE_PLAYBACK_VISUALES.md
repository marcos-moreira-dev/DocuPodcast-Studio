# DOC-UX-HF9B — rail por frase, playback de selección y visuales Word

Esta tanda corrige la experiencia de Documento después de HF9A.

## Contrato de producto

- En Documento, **frase y fragmento son equivalentes para la operación diaria**.
- El rail derecho Visual lista cada frase narrable del documento, aunque no tenga imagen asignada.
- Asociar una imagen a una frase actualiza la tarjeta exacta de esa frase.
- Al hacer clic en una tarjeta del rail, se selecciona la frase exacta en la hoja y el sidebar izquierdo debe reflejar sus capas.
- Reproducir selección debe iniciar desde la unidad/cue correspondiente a esa frase, no desde el inicio del párrafo.
- Durante playback no debe resaltarse todo el párrafo si existe una unidad/oración activa.
- Las imágenes y tablas internas del DOCX se conservan como bloques visuales no narrables; el perfil de lectura no debe convertirlas en `IGNORED`.
- LaTeX/fórmulas siguen solo como detección/notificación, sin render matemático.

## Cambios técnicos

- Nuevo `DocumentFragmentRailPresentation` para tarjetas de frase.
- Nuevo `DocumentRailProjectionFactory` para proyectar rail de frases desde documento + guion + capas + manifest.
- Nuevo `DocumentPlaybackSelectionResolver` para resolver la cue exacta de la frase seleccionada.
- `DocumentMediaRailView` deja de depender del storyboard por párrafo y usa `documentFragmentRailPresentations()`.
- `ApplyReadingProfileUseCase` preserva `IMAGE_NOTICE` y `TABLE_NOTICE` como visuales fuente no narrables.
- `DocumentWorkspaceView` suprime el highlight de bloque durante reproducción por unidad/oración.
- Diálogos de progreso/fuente portable reciben padding/ancho mínimo para evitar texto pegado al borde.
- Configuración muestra el motor de voz activo para aclarar si Voz IA avanzada quedó seleccionada.

## Validación focal

- `DocUxHf9BSentenceRailAndPlaybackSourceTest`.
- Revalidación de tests fuente que fallaban en el diagnóstico 19:02:49.
- Smoke focal de DOCX `instinto-creativo/source.docx`: 10 bloques, 1 imagen, 0 ignorados tras perfil académico.
