# PF3A — Biblioteca de voces con acciones reales sobre muestras

## Objetivo

La vista **Vista > Voces** deja de comportarse como escaparate. La biblioteca administra voces y muestras reales; la asignación a fragmentos sigue perteneciendo a Documento.

## Cambios de UX

- El panel izquierdo permite importar, grabar, reproducir, exportar y eliminar la muestra del tono seleccionado.
- El panel central explica que las acciones de la biblioteca son reales y que Documento asigna voces a fragmentos.
- La prueba generada queda separada de reproducir muestra original.
- La voz del usuario se presenta como `Mi voz`, no como pendiente/placeholder visible.
- El narrador prediseñado puede generar una prueba con la voz base del motor cuando no hay muestra humana registrada.

## Validación esperada

- `VoiceLibraryWorkspaceView` expone botones `Reproducir muestra`, `Exportar muestra…` y `Eliminar muestra`.
- `DocuPodcastShellViewModel` ofrece métodos para reproducir, exportar y eliminar muestras por tono.
- `VoiceApplicationServices` cablea `DownloadVoiceReferenceSampleUseCase` y `DeleteVoiceReferenceSampleUseCase`.
