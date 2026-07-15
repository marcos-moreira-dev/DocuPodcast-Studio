# MOTOR-SMOKE4R-HF5 — voz local simple no contamina Voz IA avanzada

## Problema corregido

Después de preparar o seleccionar **Voz local simple**, la configuración operativa podía conservar `voiceProfileId = voz-local-simple`. La inspección de **Voz IA avanzada** reutilizaba ese mismo identificador y buscaba una muestra inexistente como si fuera un speaker avanzado:

```text
models/tts/xtts/speakers/voz-local-simple.wav
```

Eso hacía que una descarga correcta del modelo avanzado apareciera como pendiente por una voz neutral equivocada.

## Decisión

La inspección y la plantilla de comando de **Voz IA avanzada** ya no interpretan la voz de **Voz local simple** como muestra avanzada. Si el motor activo no es la voz avanzada, o si el identificador es `VOC-NARRATOR`/`voz-local-simple`, se usa la muestra avanzada estándar:

```text
models/tts/xtts/speakers/voz-por-defecto.wav
```

## Alcance

- No se modifica la descarga del modelo.
- No se modifica la preparación de Python local.
- No se toca playback ni Documento.
- Se corrige solo la resolución de la muestra neutral avanzada.

## Guardarraíles

- `InspectXttsSetupReadinessUseCaseTest.ignoresLocalSimpleVoiceWhenInspectingAdvancedVoiceSetup`
- `XttsTtsCommandTemplateTest.localSimpleVoiceDoesNotLeakIntoAdvancedVoiceSpeakerPath`
- `AdvancedVoiceLocalSimpleLeakHf5SourceTest`
