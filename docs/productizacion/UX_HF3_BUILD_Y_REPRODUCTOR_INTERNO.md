# UX-HF3 — Build verde y reproductor interno WAV

Corrige el fallo de compilación en `SettingsDialog` causado por strings multilínea mal escapados y refuerza la reproducción interna de WAV por fragmentos.

## Cambios

- El mensaje de configuración inicial usa saltos de línea válidos en Java.
- La UI evita nombres técnicos de motores en la experiencia normal.
- `JavaSoundSegmentAudioPlayer` intenta abrir WAV en PCM compatible y reporta formato/causa humana si no puede reproducir.
- El límite de `DocuPodcastShellViewModel` vuelve a quedar bajo el guardarraíl RF2.

## Criterio

La app debe compilar antes de cualquier smoke funcional. Si el WAV se genera pero no suena, el estado visible debe explicar si falló el reproductor interno.
