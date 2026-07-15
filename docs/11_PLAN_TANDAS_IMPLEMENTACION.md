# Plan de implementación por tandas

## Tanda 1 — Onboarding técnico

Estado: aplicada en este repositorio.

Incluye:

- scaffolding JavaFX;
- pantalla Welcome;
- CSS claro;
- documentación viva;
- handoff técnico.

## Tanda 2 — Proyecto `.docupodcast.json`

Implementar:

- `DocuPodcastProject`;
- metadata;
- `ProjectKind` y `ProjectStatus`;
- reader/writer JSON;
- asset catalog con rutas relativas;
- guardar/abrir proyecto;
- tests de roundtrip.

## Tanda 3 — Importador DOCX mínimo

Implementar:

- Apache POI;
- extracción de párrafos/títulos/listas/tablas simples/imágenes;
- `ReadableDocument`;
- `DocumentWorkspace` real;
- perfil de lectura básico.

## Tanda 4 — Guion narrable

Implementar:

- `NarrationScriptDocument`;
- segmentos;
- workspace de guion;
- validación básica;
- export Markdown inicial.

## Tanda 5 — Audio mock + cola

Implementar:

- `AudioJob`;
- `AudioJobStatusDto`;
- progreso/ETA;
- cancelación;
- WAV fake;
- logs.

## Tanda 6 — Motor TTS real

Implementar gateway real según decisión técnica.

## Tanda 7 — Persistencia de jobs y reanudación

Implementar job folders, manifests y retry.

## Tanda 8 — Voces/personajes

Voces prediseñadas, propias/autorizadas y asignación por segmento.

## Tanda 9 — Storyboard vivo básico

Una imagen por segmento, canvas visual y playback sincronizado.

## Tanda 10 — Exportaciones

Podcast, guion Markdown, paquete proyecto, reporte diagnóstico.
