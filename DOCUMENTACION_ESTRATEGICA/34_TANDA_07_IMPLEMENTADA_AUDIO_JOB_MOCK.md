# Estrategia — Tanda 7 Audio job mock

La tanda 7 separa la UX de audio del motor TTS real. Esta decisión reduce riesgo: primero se valida la cola, progreso, ETA y salida por segmentos; luego se conecta XTTS/Piper/Python/otro motor detrás del mismo contrato.

## Contrato estratégico

```text
UI → AudioApplicationServices → AudioGenerationGateway → implementación concreta
```

La implementación actual es mock y escribe silencio, pero la app ya opera como si hubiera un job real.

## Por qué era necesaria

- Los documentos serán grandes.
- La generación puede tardar minutos.
- El usuario necesita ver progreso, ETA y segmento actual.
- El futuro motor real no debe mezclarse con JavaFX.
- Se requiere salida por segmento para reintentos y playback.

## Riesgo mitigado

Antes de TTS real ya se prueban:

- no bloqueo de UI;
- cancelación cooperativa;
- carpeta por job;
- WAV por segmento;
- manifest;
- workspace de audio.
