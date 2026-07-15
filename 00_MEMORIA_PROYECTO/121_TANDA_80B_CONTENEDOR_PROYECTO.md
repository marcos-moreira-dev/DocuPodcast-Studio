# Tanda 80B — Carpeta contenedora de proyecto

## Objetivo

Cerrar una regla operativa crítica antes de ampliar la entrada de media: un proyecto DocuPodcast no debe guardarse como un archivo suelto que derrama `source/`, `document/`, `script/`, `storyboard/`, `assets/`, `jobs/` o `exports/` directamente en el Escritorio o en cualquier carpeta elegida por el usuario.

A partir de esta tanda, la acción **Guardar como** resuelve la selección del usuario hacia una carpeta contenedora:

```text
Escritorio/Obra.docupodcast.json
→ Escritorio/Obra/Obra.docupodcast.json
```

Si el usuario ya está guardando dentro de la carpeta correcta, la ruta se conserva:

```text
Escritorio/Obra/Obra.docupodcast.json
→ Escritorio/Obra/Obra.docupodcast.json
```

## Cambios

- Se agrega `ProjectContainerPathPolicy` en `application.project`.
- `DocuPodcastShellView.handleSaveProjectAs()` usa esa política antes de delegar al ViewModel.
- El título del chooser comunica que se guardará en carpeta contenedora.
- Se agregan tests de política y guardarraíl fuente.

## Alcance

No se toca el contrato de apertura de proyectos existentes. Si existe un proyecto legado fuera de carpeta contenedora, puede seguir abriéndose y guardándose en su ruta actual. La normalización aplica a **Guardar como**, que es donde se define la estructura nueva del proyecto.

## Razón de producto

DocuPodcast manejará documentos fuente, MP3, WAV, imágenes, videos usados para extraer audio, jobs, manifests, paquetes de exportación y evidencia de smoke. Por eso el proyecto debe comportarse como una carpeta de trabajo autocontenida, no como un único archivo que deja recursos sueltos alrededor.
