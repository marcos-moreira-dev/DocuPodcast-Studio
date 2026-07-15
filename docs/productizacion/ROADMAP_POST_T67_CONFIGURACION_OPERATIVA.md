# Roadmap post T67 — configuración operativa, rediseño y cierre

## Base actual

T67 deja la configuración operativa como contrato ejecutable: persistencia, validación, servicio de aplicación y wiring en bootstrap para TTS/STT.

## Tandas pendientes

### T68 — rediseño UI aplicado

Aplicar los principios del lector narrado: Documento primero, menos scaffolding visible, botones por intención, métricas técnicas en Diagnóstico y configuración avanzada fuera del flujo normal.

### T69 — storyboard/video operativo

Conectar mejor la regla: imagen asociada a texto dura lo que dura el audio leído. Mantener paquete renderizable si FFmpeg no está completo, o MP4 real cuando FFmpeg esté disponible.

### T70 — smoke integral

Probar Word simple, Word largo, documento técnico con tablas/imágenes, diálogo/teatro, capas, refrescar contenido, audio, STT y video.

### T71 — packaging / release candidate

App-image/MSI, hashes, manifiestos, licencias, FFmpeg/Modelos documentados, guía de instalación y limitaciones conocidas.

## Advertencia

No saltar a RC sin T70. Una base con tests verdes no reemplaza el smoke manual de producto.
