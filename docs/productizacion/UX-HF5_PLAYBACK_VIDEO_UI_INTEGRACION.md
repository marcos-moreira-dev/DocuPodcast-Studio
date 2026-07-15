# UX-HF5 + PLAYBACK-HF5 — integración de playback, video local y pulido visual

Estado: implementada sobre UX-HF4/PLAYBACK-HF4.

## Motivo

Las pruebas de usuario confirmaron tres problemas de producto: la exportación WAV funcionaba, pero la reproducción interna por fragmentos no encontraba o no usaba correctamente los audios generados; los botones de transporte seguían ocupando espacio con texto; y la preparación de video local aún no era suficientemente concreta.

## Cambios

- La construcción del manifest de playback ahora contempla unidades de render TTS generadas por id de unidad, no solo audio por id de segmento.
- El shell reconstruye el manifest cuando el job de audio gana segmentos, evitando quedarse con un manifest parcial obsoleto.
- Los controles secundarios de la playbar usan iconos PNG con tooltip: pausar, reanudar, detener, fragmento anterior, siguiente fragmento y refrescar.
- Los diálogos de confirmación se muestran con ancho suficiente y texto multilínea para evitar cortes con puntos suspensivos.
- La pantalla de inicio incorpora el logo transparente como marca de fondo.
- Video local agrega acción explícita de preparación/importación desde Configuración.
- Se actualizan guardarraíles de source tests para el nuevo contrato.

## Validación local en entorno ChatGPT

- `javac --release 21` de dominio + application: OK.
- `javac --release 21` de infraestructura playback: OK.
- 16 métodos de source tests focales con stubs JUnit: OK.
- Maven completo no se ejecutó porque el entorno no tiene `mvn`.
