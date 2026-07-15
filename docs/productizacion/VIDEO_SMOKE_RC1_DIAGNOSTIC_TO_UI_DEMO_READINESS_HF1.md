# VIDEO-SMOKE-RC1 + DIAGNOSTIC-TO-UI-HF1 + DEMO-READINESS-HF1

Tanda enfocada en cerrar tres superficies operativas sin agregar relleno visual.

## VIDEO-SMOKE-RC1

Se agrega `InspectFinalVideoSmokeReadinessUseCase` y `scripts/38-smoke-video-final.bat` para que el smoke MP4 final no empiece un render largo si el proyecto o FFmpeg todavía no están listos.

El smoke revisa readiness de `FINAL_VIDEO_MP4` y evidencia de Video local/FFmpeg antes de permitir la prueba.

## DIAGNOSTIC-TO-UI-HF1

El reporte diagnóstico exportado ya no queda solo como status bar. `DiagnosticUserDecisionFactory` crea un aviso operativo para que el usuario sepa dónde quedó el reporte y para qué sirve.

## DEMO-READINESS-HF1

El selector de ejemplos muestra qué entrega cada demo antes de crearla. El demo teatral puede indicar que trae imágenes y asociaciones visuales listas para el rail del Documento.

Regla: cada texto visible responde qué puede hacer el usuario o qué estado necesita conocer antes de continuar.
