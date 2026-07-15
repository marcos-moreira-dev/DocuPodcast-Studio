# MOTOR-PLAYCONF1 — confirmación de reproducción de Voz IA avanzada

## Objetivo

Cerrar el ciclo de preparación de Voz IA avanzada: no basta con descargar/verificar el modelo ni generar un WAV de prueba. La app debe poder reproducir ese WAV con el reproductor interno y marcar la prueba como confirmada solo cuando la reproducción termina naturalmente.

## Cambios

- Se agrega `ConfirmXttsSmokePlaybackUseCase`.
- `SettingsApplicationServices` expone `confirmXttsSmokePlayback`.
- `ApplicationServicesFactory` cablea un reproductor Java Sound separado para esta prueba, evitando interferir con la cola de playback del documento.
- Configuración agrega la acción **Reproducir prueba** en Voz IA avanzada.
- La confirmación se escribe en `runtime/tts/xtts-smoke/xtts-readiness-smoke.json` con:
  - `playbackConfirmed: true`
  - `playbackConfirmedAt`
  - `playbackConfirmedBy: app-internal-player`

## Regla de producto

Una Voz IA avanzada completamente verificada requiere:

1. Runtime preparado.
2. Modelo descargado/verificado.
3. Voz neutral disponible.
4. WAV real generado.
5. Reproducción confirmada dentro de la app.

## Fuera de alcance

- No se toca GPU/CPU.
- No se toca exportación de video.
- No se cambia el playback de documentos.
- No se modifica la Vista Voces.
