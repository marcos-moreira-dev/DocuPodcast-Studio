# 25 — Tanda 2 plan: Proyecto `.docupodcast.json`

## Objetivo

Implementar persistencia mínima del proyecto y assets relativos.

## Clases dominio

```text
DocuPodcastProject
ProjectMetadata
ProjectKind
ProjectStatus
ProjectAssetCatalog
ProjectAssetReference
ProjectAssetKind
DocuPodcastViewState
```

## Application

```text
ProjectRepository
CreateProjectUseCase
SaveProjectUseCase
OpenProjectUseCase
ValidateProjectPayloadUseCase
RegisterProjectAssetUseCase
```

## Infrastructure

```text
DocuPodcastProjectFileRepository
DocuPodcastProjectJsonReader
DocuPodcastProjectJsonWriter
DocuPodcastProjectFormat
DocuPodcastPayloadConsistencyValidator
```

## Tests

```text
DocuPodcastProjectRoundTripTest
ProjectAssetRelativePathTest
DocuPodcastJsonVersionTest
PayloadConsistencyValidatorTest
```

## Resultado

Se puede crear, guardar, abrir y validar un proyecto vacío o con metadata básica. Assets solo admiten rutas relativas seguras.
