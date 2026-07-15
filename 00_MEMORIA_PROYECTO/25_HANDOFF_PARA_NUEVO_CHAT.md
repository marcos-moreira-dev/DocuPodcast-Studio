# Handoff para nuevo chat o nuevo agente

Este archivo resume lo esencial para continuar el proyecto.

## Producto

DocuPodcast Studio es una app JavaFX que convierte documentos Word/DOCX en guiones narrables, audio por segmentos y storyboards vivos.

## Decisiones claves

- Java 21 Eclipse Temurin.
- Maven Toolchain.
- JavaFX.
- Word/DOCX es entrada prioritaria.
- Markdown es puente IA/humano.
- `.docupodcast.json` es proyecto editable.
- DMS es referencia de shell/UI/scaffolding.
- Fractal es referencia de jobs/progreso/batch.
- Guion es estructurado, no canvas.
- Storyboard sí usa canvas.
- Audio se genera por segmentos y debe ser reanudable.

## Siguiente tarea recomendada

Implementar Tanda 2: proyecto `.docupodcast.json` mínimo.

## No olvidar

- No prometer emociones si motor no soporta.
- No clonar voces sin permiso.
- No meter audios en JSON principal.
- No usar rutas absolutas para assets del proyecto.
- No bloquear UI con jobs largos.
- No convertir guion en canvas.
- No esconder Word detrás de Markdown.

## Archivos guía

- `20_TANDAS_IMPLEMENTACION_DETALLADAS.md`.
- `05_ARQUITECTURA_FINAL.md`.
- `07_WORD_DOCX_FIRST.md`.
- `11_AUDIO_JOBS_TTS.md`.
- `13_PERSISTENCIA_ASSETS.md`.
