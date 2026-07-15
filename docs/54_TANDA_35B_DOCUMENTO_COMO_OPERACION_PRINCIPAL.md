# Tanda 35B — Documento como pantalla principal de operación

## Propósito

Alinear DocuPodcast Studio con la experiencia principal de producto: el usuario no profesional abre un Word/DOCX, lee el documento y pulsa una acción principal para escucharlo. La infraestructura de guion, audio, voces, storyboard y jobs sigue existiendo, pero la pantalla principal debe ser el Documento.

## Cambios

- Se agrega la capacidad `LISTEN_DOCUMENT`.
- La toolbar global muestra `Escuchar documento` dentro del grupo Documento.
- La toolbar contextual del workspace Documento prioriza `Escuchar documento` y deja `Crear guion` como acción secundaria.
- `DocuPodcastShellViewModel.listenToDocument()` actúa como entrada de alto nivel: valida documento, prepara guion si hace falta, reproduce si existe manifest o envía generación de audio si el proyecto está guardado.
- `DocumentWorkspaceView` incorpora una franja de operación con un único botón principal `Escuchar documento`.
- El documento permanece como pantalla de operación mientras se prepara audio o se reproduce.

## Límites

Esta tanda no implementa selección por oración, `PerformanceSpan` parcial, asignación de Pepita/voz/audio a rangos ni video simple. Solo fija la experiencia principal y la acción de entrada.
