# T122-C01 — Encapsular Script como PreparedReadingProjection

## Objetivo

Introducir una frontera de aplicación llamada `PreparedReadingProjection` para que los flujos de producto hablen de documento y lectura preparada, mientras el modelo histórico `NarrationScriptDocument` queda como payload interno de compatibilidad.

## Decisión de producto

DocuPodcast no permite subir ni gestionar guiones como categoría de usuario. El usuario abre documentos Word, PDF, Markdown o TXT, y la aplicación los prepara para lectura. Cualquier uso de `script`, `segment` o `NarrationScriptDocument` debe considerarse compatibilidad técnica temporal para audio, render, playback y persistencia.

## Cambios realizados

- Se agrega `application.reading.PreparedReadingProjection`.
- Se agrega `application.reading.BuildPreparedReadingProjectionUseCase`.
- `ScriptApplicationServices` expone `buildPreparedReadingProjection()`.
- `ApplicationServicesFactory` construye el nuevo caso de uso reutilizando el builder interno de compatibilidad.
- `DocumentNarrationCoordinator` prepara lectura mediante `buildPreparedReadingProjection()` y solo desempaqueta el payload interno para compatibilidad.
- `AudioGenerationRequest` acepta `PreparedReadingProjection` como entrada de producto.
- `BuildNarrationRenderPlanUseCase` acepta `PreparedReadingProjection` como entrada de producto.
- `NarrationScriptDocument` y `BuildNarrationScriptUseCase` quedan documentados explícitamente como compatibilidad interna.
- Mensajes visibles de preparación hablan de “lectura preparada” y “fragmentos”.

## Alcance deliberadamente limitado

No se elimina todavía `domain/script` ni `application/script`, porque audio, render, playback y round-trip de proyecto siguen dependiendo de esos tipos. Esta tanda crea la frontera para migraciones posteriores sin romper compatibilidad.

## Criterios de aceptación

- Existe `PreparedReadingProjection` como frontera clara.
- Los flujos de Documento usan la frontera de lectura preparada.
- El usuario no ve Guion como categoría de producto.
- Los tipos legacy quedan documentados como internos.
- Tests fuente protegen la nueva frontera.
