# MOTOR-SMOKE4R-HF4 — descarga completa y preparación pendiente

## Contexto

El reporte real `models/tts/xtts/download-diagnostics.txt` confirmó un caso importante: todos los recursos del modelo de voz avanzada pueden descargarse correctamente, pero la configuración inicial todavía puede quedar pendiente por pasos posteriores de ejecución local.

En ese caso la aplicación no debe tratar el problema como descarga fallida. Debe separar claramente:

1. modelo descargado;
2. modelo usable por inspección local;
3. entorno local de ejecución preparado;
4. prueba WAV generada;
5. reproducción confirmada.

## Cambios

- `SettingsDialog` detecta cuando el modelo descargado es usable pero el entorno local de ejecución sigue pendiente.
- El mensaje visible ahora dice que el modelo fue descargado correctamente y lista lo que falta preparar.
- El diálogo de progreso usa un área de texto de detalle para que el usuario pueda copiar el mensaje completo.
- Se muestra la ruta del reporte de preparación local cuando corresponde.
- Se mantiene el reporte de descarga para soporte.

## Validación esperada

Si el reporte de descarga indica `success=true`, `Fallidos=[]` e inspección usable, el usuario debe ver un mensaje de continuación/preparación pendiente, no un error genérico de descarga.
