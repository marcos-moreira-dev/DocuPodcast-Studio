# Tandas restantes después de Tanda 3/4

## Estado actual

Ya existe una cadena funcional:

```text
Abrir Word/DOCX
→ extraer bloques en orden
→ detectar estilos, listas, imágenes y tablas
→ emitir diagnóstico documental
→ mostrar Documento con panel lateral inicial
→ seleccionar bloques
→ guardar proyecto
→ copiar source/notas.docx
→ crear document/document.json enriquecido
→ registrar assets relativos
```

## Tandas pendientes actualizadas

### Tanda 4.5 — Document Workspace completo + SideDock genérico

La Tanda 4 quedó avanzada, pero todavía falta formalizarla:

- SideDock genérico reutilizable estilo DMS.
- Árbol jerárquico por títulos/subtítulos.
- Filtros por tipo de bloque.
- Búsqueda textual en documento.
- Acciones rápidas sobre bloque:
  - marcar como título;
  - marcar como subtítulo;
  - marcar como párrafo;
  - marcar como lista;
  - ignorar en audio.
- Persistir selección y scroll en `viewState`.

### Tanda 5 — Reading Profile

- `ReadingProfile`.
- Reglas para títulos y subtítulos.
- Reglas por estilo Word, numeración, negrita y longitud.
- Política de imágenes.
- Política de tablas.
- Vista previa de reclasificación.

### Tanda 6 — Guion narrable

- `NarrationScriptDocument`.
- `NarrationSegment`.
- Construcción de guion desde `ReadableDocument`.
- Validación inicial.
- Workspace Guion estructurado.

### Tanda 7 — Audio job mock

- `AudioJob`.
- `AudioJobStatusDto`.
- Cola visible.
- ProgressBar.
- ETA.
- Cancelación cooperativa.
- WAV fake por segmento.

### Tanda 8 — Gateway TTS real

- `AudioGenerationGateway`.
- Adaptador real XTTS/Piper/Python worker según decisión.
- WAV por segmento.
- Unión de audio.
- Diagnóstico de motor.

### Tanda 9 — Reanudación de jobs

- `job.json`.
- `segments-status.json`.
- `audio-manifest.json`.
- Reabrir job incompleto.
- Reintentar fallidos.

### Tanda 10 — Voice Library

- Voces prediseñadas.
- Voz propia.
- Voces autorizadas.
- Personajes.
- Estilos de interpretación.
- Prueba de voz.

### Tanda 11 — Storyboard básico

- Media assets de imagen.
- Una imagen por segmento.
- Canvas storyboard.
- Preview visual.
- Binding imagen-segmento.

### Tanda 12 — Playback sincronizado

- `PlaybackManifest`.
- Reproducción por segmento.
- Resaltado de texto.
- Resaltado de escena/imagen.

### Tanda 13 — Exportaciones

- Guion Markdown.
- Podcast WAV/MP3.
- Paquete de proyecto.
- Preview storyboard.
- Reporte diagnóstico.

### Tanda 14 — Guía integrada + recursos IA

- Guía Word-first.
- Recursos IA exportables.
- `docupodcast-script-v1`.
- Ejemplos académico/teatro/storyboard.

### Tanda 15 — Packaging / release candidate

- `jpackage`.
- MSI.
- Validación toolchain Temurin 21.
- Scripts release.
