# 29 — Memoria: Tanda 2 implementada

La Tanda 2 implementó la base persistente mínima de DocuPodcast Studio.

## Decisión cerrada

El proyecto editable será:

```text
.docupodcast.json + carpeta de assets/jobs/exports
```

El JSON principal guarda metadata, referencias y estado. Los archivos pesados viven fuera:

- DOCX fuente;
- imágenes;
- muestras de voz;
- WAV/MP3;
- manifests;
- logs;
- exports.

## Dominio agregado

```text
DocuPodcastProject
ProjectMetadata
ProjectKind
ProjectStatus
ProjectAssetReference
ProjectAssetCatalog
ProjectAssetKind
```

## Reglas de rutas

Los assets usan rutas relativas y portables. La política rechaza:

```text
C:/...
/home/...
\\servidor\...
http://...
https://...
file:...
../...
./...
```

Esto protege exportación, portabilidad y privacidad.

## Aplicación

Se agregaron puertos/casos de uso:

```text
ProjectRepository
CreateProjectUseCase
SaveProjectUseCase
OpenProjectUseCase
ValidateProjectPayloadUseCase
RegisterProjectAssetUseCase
RemoveProjectAssetUseCase
```

## Infraestructura

Se agregó un repositorio de archivo:

```text
DocuPodcastProjectFileRepository
```

con reader/writer propios:

```text
DocuPodcastProjectJsonReader
DocuPodcastProjectJsonWriter
```

## Validación

`ValidateProjectPayloadUseCase` comprueba que el `ProjectKind` declarado no prometa payloads inexistentes.

Ejemplo:

- `DOCUMENT_ONLY` exige documento fuente/importado.
- `NARRATION_SCRIPT` exige guion.
- `STORYBOARD` exige guion + storyboard manifest.
- `AUDIO_PROJECT` exige guion + audio.

## Estado de UI

La UI todavía no expone abrir/guardar proyecto. Por eso se agrega Tanda 2.5.

## Próximo paso recomendado

```text
Tanda 2.5 — Integración UI de proyecto/session.
```

Luego:

```text
Tanda 3 — Importador DOCX/Word real.
```
