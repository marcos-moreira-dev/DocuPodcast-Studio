# UX-HF4 + PLAYBACK-HF4 — diálogos legibles, setup encadenado y navegación de fragmentos

Base: UX-HF3 verde reportada por el usuario.

## UX-HF4

- Los cuadros de confirmación de configuración usan contenido con `wrapText`, ancho mínimo y saltos de línea reales para evitar puntos suspensivos.
- Configuración inicial mantiene la preparación de Voz IA avanzada y, si queda lista, pregunta al usuario si también desea preparar Voz local simple.
- El usuario puede cancelar cualquiera de las dos preparaciones sin bloquear la app.
- Los mensajes visibles siguen usando nombres de producto: Voz IA avanzada, Voz local simple y Video local.

## PLAYBACK-HF4

- La barra flotante de lectura añade botones para `Fragmento anterior` y `Siguiente fragmento`.
- Si hay un fragmento sonando, saltar al anterior/siguiente detiene el audio actual y reproduce el fragmento objetivo.
- La reproducción interna de WAV cambia de `Clip` a `SourceDataLine` para transmitir audio PCM por streaming y reducir fallos silenciosos con formatos generados por motores locales.
- La lógica de navegación vive en `PlaybackFragmentNavigator` para no aumentar deuda del `DocuPodcastShellViewModel`.

## Validación esperada

- `scripts\99-diagnostico-completo.bat` debe quedar verde.
- En Documento, con audio ya generado por fragmentos, `Fragmento anterior` y `Siguiente fragmento` deben saltar entre segmentos.
- Si el reproductor interno no puede abrir un WAV, debe mostrar un mensaje concreto en la barra de estado.
