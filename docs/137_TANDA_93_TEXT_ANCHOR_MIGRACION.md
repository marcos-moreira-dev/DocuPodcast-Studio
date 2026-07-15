# Tanda 93 — TextAnchor mínimo + migración controlada

T93 agrega el contrato mínimo de anclas estables para capas narrativas. El formato `.docupodcast.json` sube a versión 2, manteniendo lectura de proyectos v1.

## Archivos clave

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/TextAnchor.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/TextAnchorConfidence.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/TextAnchorStatus.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/assignment/NarrativeLayerAssignment.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectFormat.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonReader.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonWriter.java
```

## Decisiones

- `TextAnchor` guarda rango, texto seleccionado opcional, hash del texto, contexto antes/después, hash de snapshot, confianza y estado.
- Las capas antiguas se migran como anclas de baja confianza que requieren revisión.
- Se conserva compatibilidad con los campos legacy de rango documental para no romper herramientas actuales.

## Próxima tanda

T94 — Job común de procesos largos.
