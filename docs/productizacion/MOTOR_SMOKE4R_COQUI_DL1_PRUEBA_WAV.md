# MOTOR-SMOKE4R / COQUI-DL1 — prueba WAV real de Voz IA avanzada

## Motivo

La preparación de Voz IA avanzada no debe declararse lista solo porque existan archivos descargados. El usuario reportó un riesgo real: la descarga puede tardar mucho, terminar en error o dejar al flujo en un estado ambiguo. Esta tanda separa explícitamente:

1. Runtime local preparado.
2. Modelo descargado/importado.
3. Modelo verificado.
4. Voz neutral disponible.
5. Motor seleccionable.
6. Prueba WAV real generada.
7. Reproducción confirmada dentro de la app.

## Cambios técnicos

- Se agregó `RunXttsReadinessSmokeUseCase` para ejecutar una prueba corta de síntesis real con Voz IA avanzada.
- Se agregó `XttsSmokeTestReport` como contrato de estado de prueba.
- Se agregó `InspectXttsSmokeTestUseCase` para inspeccionar el último WAV de prueba en `runtime/tts/xtts-smoke`.
- `SettingsApplicationServices` expone los casos de uso de smoke de Voz IA avanzada.
- `ApplicationServicesFactory` cablea el smoke con `infrastructure.voiceTestSynthesisGateway()` para usar el mismo gateway real de prueba de voz.
- `SettingsDialog` agrega botón `Probar` en Voz IA avanzada.
- `InspectAiEnginesPreflightUseCase` ya distingue entre descargado/seleccionable, WAV generado y reproducción confirmada.
- `DownloadXttsOfficialModelUseCase` soporta reanudación parcial con header HTTP `Range`, para no perder descargas grandes si queda un `.download` parcial.

## Alcance del smoke

El botón `Probar` genera:

```text
runtime/tts/xtts-smoke/xtts-readiness-smoke.txt
runtime/tts/xtts-smoke/xtts-readiness-smoke.wav
runtime/tts/xtts-smoke/xtts-readiness-smoke.json
```

El manifiesto usa el esquema:

```text
docupodcast-xtts-readiness-smoke-v1
```

## Límite honesto

Esta tanda genera la prueba WAV real desde Configuración. La reproducción confirmada queda modelada en el contrato (`playbackConfirmed`) y el preflight la reporta como pendiente si aún no existe confirmación. La confirmación automática de reproducción puede cerrarse en una tanda posterior si se decide integrar un botón de reproducción directa en Configuración o marcar la reproducción desde Vista Voces.

## Fuera de alcance

- No se cambia playback por chunks.
- No se cambia Documento.
- No se cambia Vista Voces.
- No se implementa GPU real.
- No se cambia FFmpeg/video.
