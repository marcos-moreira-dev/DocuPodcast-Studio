# Tanda 40 — Botón inteligente: Escuchar documento / Reproducir desde aquí

## Objetivo

Convertir la acción principal del Documento en un botón contextual único:

```text
sin selección relevante → Escuchar documento
con bloque/segmento seleccionado → Reproducir desde aquí
```

La pantalla principal sigue orientada a usuarios no profesionales: una sola acción visible dirige el flujo de lectura, mientras la fábrica interna decide si debe preparar guion, generar audio por chunks o reproducir un manifest existente.

## Cambios principales

- `DocuPodcastShellViewModel` expone `documentPrimaryActionLabelProperty()` y `documentPrimaryActionHintProperty()`.
- Se agrega `runDocumentPrimaryAction()` como acción pública de alto nivel.
- Se agrega `selectDocumentBlock(String)` para que el Documento sea el ancla transversal de operación.
- `DocumentWorkspaceView` enlaza la selección de bloque con el view-model y usa `PrimaryActionStrip` con texto/hint dinámicos.
- `MainToolbarView` enlaza el botón global/contextual a la etiqueta dinámica.
- `PrimaryActionStrip` acepta `ObservableValue<String>` para reutilizar el mismo componente en acciones contextuales.
- El prebuffer de Tanda 39 respeta el punto de inicio seleccionado cuando el manifest parcial ya contiene ese segmento.

## Decisiones de producto

La selección no escribe acotaciones en el Word original. El bloque seleccionado solo define el punto operativo del proyecto DocuPodcast.

Si todavía falta guion o audio, el botón contextual conserva una experiencia simple: prepara lo necesario y continúa desde el punto elegido cuando sea posible.

## Fuera de alcance

- Selección exacta por oración con arrastre.
- Asignación real de voz/audio/emoción/imagen a rangos parciales.
- Mini rail multimedia.
- Descarga de modelos TTS/STT.
- Exportación de video simple.
