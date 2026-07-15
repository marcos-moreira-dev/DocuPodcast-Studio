# Advanced workspace degradation — T81G

## Contrato

T81G declara que DocuPodcast Studio V1 tiene una superficie principal y varias superficies avanzadas. La superficie principal es `Documento`; las superficies avanzadas se mantienen como herramientas, no como navegación cotidiana.

## Primary surfaces

- `WELCOME_HOME`
- `DOCUMENT_READER`

## Advanced surfaces

- `SCRIPT_EDITOR`
- `AUDIO_JOBS`
- `VOICE_LIBRARY`
- `STORYBOARD`

## Reglas

1. `primaryNavigation()` debe ser `true` solo para Inicio y Documento.
2. Las vistas avanzadas pueden seguir implementadas para soporte y pruebas.
3. El usuario puede acceder a ellas desde `Herramientas`, con etiqueta `(avanzado)`.
4. Si un proyecto se guardó en una vista avanzada, al reabrir debe volver a `DOCUMENT_READER`.
5. Ninguna vista avanzada debe volver a la toolbar global como botón permanente.

## Razón UX

El usuario no está comprando una cabina de producción. Está abriendo un documento para escucharlo, estudiar, asociar imágenes/audio y exportar. Los detalles internos de guion, jobs y storyboard completo son útiles, pero no deben dominar la atención.
