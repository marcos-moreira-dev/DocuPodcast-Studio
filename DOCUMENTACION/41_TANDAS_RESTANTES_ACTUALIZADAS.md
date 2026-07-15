# Tandas restantes actualizadas después de Tanda 3/4

Esta hoja reemplaza el mapa anterior de tandas desde el estado actual.

## Estado actual

Ya existe cadena técnica funcional:

```text
Abrir Word/DOCX
→ extraer bloques en orden
→ detectar estilos, listas, imágenes y tablas
→ mostrar Documento con SideDock mínimo
→ guardar proyecto
→ copiar source/notas.docx
→ crear document/document.json enriquecido
→ registrar assets relativos
```

## Tandas pendientes

### Tanda 4.5 — SideDock/document workspace completo

La Tanda 4 ya fue avanzada, pero falta cerrarla con:

- SideDock genérico reutilizable.
- Árbol jerárquico de títulos/subtítulos.
- Filtro por tipo de bloque.
- Búsqueda textual dentro del documento.
- Selección de bloque sincronizada con el reader central.
- Acciones rápidas: marcar como título, subtítulo, párrafo, ignorar.

### Tanda 5 — Reading Profile

- `ReadingProfile`.
- Reglas de título/subtítulo.
- Reglas por estilo Word, tamaño, negrita y numeración.
- Política de imágenes.
- Política de tablas.
- Vista previa de clasificación.

### Tanda 6 — Guion narrable

- `NarrationScriptDocument`.
- `NarrationSegment`.
- Construcción de guion desde `ReadableDocument`.
- Segmentos por bloque/sección.
- Workspace Guion.
- Validación básica.

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
- Unión WAV.
- Diagnóstico del motor.

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
