# 29 — Tanda 6 plan: TTS real

## Objetivo

Integrar un motor de voz real sin acoplar la UI.

## Opciones

```text
PiperProcessGateway
XttsPythonWorkerGateway
OnnxRuntimeGateway futuro
```

## Regla

El motor va detrás de `AudioGenerationGateway`.

## Configuración

```text
VoiceProfile
VoiceEngineSettings
EngineDiagnostics
```

## Requisitos

- Probar voz.
- Generar WAV por segmento.
- Manejar errores de motor.
- Registrar logs.
- No bloquear JavaFX.

## Criterio

Un guion pequeño produce WAV real por segmentos desde una voz configurada.
