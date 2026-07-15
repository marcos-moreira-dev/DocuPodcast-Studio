# Tanda 2 — Proyecto `.docupodcast.json` mínimo

## Objetivo

Implementar persistencia mínima de proyecto editable.

## Dominio

Crear:

- `DocuPodcastProject`;
- `ProjectMetadata`;
- `ProjectKind`;
- `ProjectStatus`;
- `ProjectAssetReference`;
- `ProjectAssetCatalog`;
- `ProjectAssetKind`.

## Application

Crear:

- `ProjectRepository`;
- `CreateProjectUseCase`;
- `SaveProjectUseCase`;
- `OpenProjectUseCase`;
- `RegisterProjectAssetUseCase`.

## Infrastructure

Crear:

- `DocuPodcastProjectFileRepository`;
- `DocuPodcastProjectJsonReader`;
- `DocuPodcastProjectJsonWriter`;
- `DocuPodcastProjectFormat`;
- `DocuPodcastPayloadConsistencyValidator`.

## UI

- Nuevo proyecto.
- Guardar.
- Abrir.
- Título de ventana con dirty state.

## Persistencia

Formato inicial:

```json
{
  "formatVersion": 1,
  "project": {},
  "assets": {},
  "view": {}
}
```

## Tests

- roundtrip JSON;
- rechazo de rutas absolutas;
- versión futura rechazada;
- assets por ID;
- no guardar rutas con `..`.
