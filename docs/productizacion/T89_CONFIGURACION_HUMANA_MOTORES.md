# T89 — Configuración humana de motores Coqui/Piper/FFmpeg

## Objetivo

T89 convierte Configuración en una superficie honesta para revisar motores reales sin llevar detalles técnicos al Documento. El producto visible se limita a:

- **Coqui/XTTS** para voz de calidad alta.
- **Piper** como respaldo rápido/liviano.
- **FFmpeg** para preparar audio/video y normalizar clips.

Whisper/STT no pertenece al producto visible DocuPodcast. La infraestructura histórica puede quedar encapsulada, pero no aparece en Configuración protagonista, toolbar, guía normal ni criterios de RC.

## Cambios

- `SettingsDialog` usa `InspectAiEnginesPreflightUseCase` para mostrar estado real de motores.
- La sección **Motores de voz y media** muestra resumen, estado, mensaje humano, siguiente acción y ruta esperada.
- No se agregan botones falsos de descarga, prueba o importación si el flujo no está implementado; la configuración no muestra botones falsos.
- Se mantiene el catálogo de Coqui/Piper como explicación de alcance, sin depender de URLs fijas.
- Se corrigen guardarraíles T88C que todavía esperaban STT/Whisper o etiquetas heredadas.
- Se refuerza el ancho mínimo de botones de Configuración para evitar truncados.

## Regla de producto

```text
Si aparece, funciona.
Si no funciona, se oculta o se presenta como estado/guía, no como botón.
```

## Próximo paso

T90 debe probar motores reales con evidencia local:

```text
Coqui/XTTS → WAV corto
Piper → WAV corto
FFmpeg → audio preparado desde archivo/video
guardar/reabrir proyecto
exportar evidencias
```
