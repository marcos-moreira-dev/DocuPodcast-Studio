# MOTOR-SMOKE4R-HF3 — observabilidad de descarga de Voz IA avanzada

## Objetivo

Corregir la preparación de Voz IA avanzada para que, cuando la descarga o verificación no complete el estado seleccionable, el usuario vea una causa accionable y pueda enviar un archivo de diagnóstico técnico.

## Cambios

- `DownloadXttsOfficialModelUseCase` ahora escribe `download-diagnostics.txt` junto a los recursos del modelo.
- El reporte incluye fuente configurada, fuente normalizada, base técnica de descarga, lista de recursos esperados, URL técnica por recurso, HTTP status, `Content-Length`, `Content-Type`, tamaños descargados, recursos fallidos, inspección final y advertencias.
- `XttsModelDownloadReport` conserva `diagnosticReport` para que la UI pueda mostrar la ruta exacta del reporte.
- `SettingsDialog` combina ahora el resultado de descarga con la inspección final de readiness. Si el modelo está descargado pero todavía falta Python local, wrapper o voz neutral, lo dice explícitamente.
- El diálogo de progreso deja de cerrar con un texto genérico cuando existe reporte técnico; pide copiar el mensaje o adjuntar el reporte.

## Contrato de producto

La aplicación no debe decir únicamente “error o interrupción” cuando falla la descarga/preparación de Voz IA avanzada. Debe indicar:

1. qué recurso falló o quedó pendiente;
2. si el modelo descargado no quedó seleccionable por otro requisito local;
3. dónde quedó el reporte técnico para soporte.

## Validación esperada

- Ejecutar `scripts\\99-diagnostico-completo.bat`.
- Abrir Configuración > Motores de voz.
- Preparar Voz IA avanzada.
- Si falla, revisar que aparezca “Reporte técnico para soporte”.
- Adjuntar `models/tts/xtts/download-diagnostics.txt` para depurar URLs, HTTP y recursos.
