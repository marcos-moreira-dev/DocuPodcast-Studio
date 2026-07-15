# 36 — Tanda 2 implementada: `.docupodcast.json` + assets relativos

Esta tanda convierte el onboarding en una base persistente real. El repositorio ya tiene dominio mínimo de proyecto, catálogo de assets, casos de uso de proyecto/assets y repositorio JSON.

## Objetivo

Crear la primera versión técnica del formato editable de DocuPodcast Studio:

```text
.docupodcast.json + carpeta de proyecto con assets relativos
```

Esto se inspira en la persistencia `.dms` de Domain Model Studio/UENS, pero con dominio propio: documentos, guion, voces, storyboard, audio y jobs.

## Entregables de código

### Dominio de proyecto

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/
├── DocuPodcastProject.java
├── ProjectMetadata.java
├── ProjectKind.java
└── ProjectStatus.java
```

`DocuPodcastProject` ya funciona como agregado raíz inicial. Guarda:

- metadata;
- catálogo de assets;
- estado mínimo de vista.

### Dominio de assets

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/assets/
├── ProjectAssetCatalog.java
├── ProjectAssetKind.java
└── ProjectAssetReference.java
```

`ProjectAssetReference` valida rutas portables:

- acepta rutas relativas;
- normaliza `\` a `/`;
- rechaza rutas absolutas Windows/Linux;
- rechaza URL;
- rechaza `.` y `..`;
- rechaza rutas vacías.

Tipos iniciales de assets:

```text
SOURCE_DOCUMENT
IMPORTED_DOCUMENT
NARRATION_SCRIPT
IMAGE
THUMBNAIL
VOICE_SAMPLE
VOICE_MODEL
AUDIO_CLIP
AUDIO_FINAL
AUDIO_MANIFEST
STORYBOARD_MANIFEST
PLAYBACK_MANIFEST
GENERATION_LOG
EXPORT
OTHER
```

### Capa de aplicación

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/application/
├── ApplicationServices.java
├── assets/
├── project/
└── services/
```

Casos de uso implementados:

- `CreateProjectUseCase`
- `SaveProjectUseCase`
- `OpenProjectUseCase`
- `ValidateProjectPayloadUseCase`
- `RegisterProjectAssetUseCase`
- `RemoveProjectAssetUseCase`

También se agregaron fachadas de familia:

- `ProjectApplicationServices`
- `AssetApplicationServices`
- `ApplicationServices`

### Infraestructura JSON

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/
├── DocuPodcastProjectFileRepository.java
├── DocuPodcastProjectFormat.java
├── DocuPodcastProjectJsonReader.java
├── DocuPodcastProjectJsonWriter.java
└── SimpleJsonParser.java
```

Formato inicial:

```json
{
  "formatVersion": 1,
  "project": {},
  "assets": { "items": [] },
  "view": {}
}
```

El reader rechaza versiones futuras de formato.

## Bootstrap actualizado

El bootstrap ahora compone:

```text
InfrastructureServicesFactory
  → DocuPodcastProjectFileRepository

ApplicationServicesFactory
  → ProjectApplicationServices
  → AssetApplicationServices
```

La UI todavía no abre/guarda proyectos desde menú. Esa integración queda para la nueva Tanda 2.5.

## Tests agregados

```text
ProjectAssetReferenceTest
ProjectAssetCatalogTest
DocuPodcastProjectTest
ValidateProjectPayloadUseCaseTest
DocuPodcastProjectFileRepositoryTest
DocuPodcastProjectJsonReaderTest
```

Cubren:

- rutas relativas;
- rechazo de rutas inseguras;
- catálogo por ID/tipo;
- inmutabilidad básica de proyecto;
- validación por `ProjectKind`;
- roundtrip JSON;
- rechazo de versiones futuras.

## Ejemplo incluido

```text
samples/minimal-project/ProyectoMinimo.docupodcast.json
```

Ese ejemplo muestra un proyecto `DOCUMENT_ONLY` que referencia un DOCX en `source/notas-ejemplo.docx` mediante ruta relativa.

## Lo que NO implementa todavía

- UI de abrir/guardar `.docupodcast.json`.
- Sesiones/tabs reales de proyecto.
- FileChooser.
- Importación DOCX real.
- Guion narrable real.
- Audio jobs.
- Storyboard.

## Nueva tanda agregada

Antes de saltar al importador Word, conviene una tanda intermedia:

```text
Tanda 2.5 — Integración UI de proyecto/session
```

Objetivo:

- Nuevo proyecto;
- Abrir proyecto `.docupodcast.json`;
- Guardar/Guardar como;
- dirty state;
- título de ventana con `*`;
- FileChooser;
- statusbar con mensajes reales;
- validación visible si el proyecto es inconsistente.

