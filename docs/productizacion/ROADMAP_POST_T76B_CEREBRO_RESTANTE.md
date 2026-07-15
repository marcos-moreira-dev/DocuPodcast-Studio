# Roadmap post T76B — cerebro restante antes del rediseño frontal

## Estado de partida

T76B deja corregido el guardarraíl documental de video render contract. No agrega capacidad funcional nueva; estabiliza la base para continuar con tandas de cerebro.

## Orden recomendado

### T77 — Integridad y reparación del proyecto

Crear un reporte de integridad no binario para fuente, documento narrable, narración interna, capas, assets, checksums, audio jobs, storyboard, video package y configuración mínima. Debe diferenciar `OK`, `CON_ADVERTENCIAS` y `REQUIERE_REPARACION`.

### T78 — Exportaciones del cerebro

Centralizar la preparación de exportaciones: bundle auditable, podcast WAV, diagnóstico, Markdown de narración, storyboard/video plan, manifiestos, hashes, limitaciones y readiness.

### T79 — Smoke automático del cerebro

Agregar un smoke automático sin JavaFX que cubra importación documental, preparación de lectura, audio mock, round-trip, capas, storyboard, exportaciones, paquete de video e integridad antes/después.

### T80A — Dispositivo de inferencia CPU/GPU y rendimiento

Modelar `AUTO`, `CPU_ONLY`, `PREFER_GPU` y `SPECIFIC_DEVICE` para TTS, STT y video render. El diagnóstico debe ser honesto: detectar o declarar no verificado, y registrar fallback.

### T80B — Entrada flexible de media

Aceptar MP3/WAV como audio importable y permitir video solo para extraer audio cuando FFmpeg esté disponible. Registrar asset original, derivado normalizado, checksum, formato, duración y estado.

### T80 — Congelación del cerebro V1

Cerrar matriz de capacidades, límites, contratos, no regresión y exclusiones explícitas: OCR, editor de video avanzado, edición Word completa, nube y colaboración.

### T81 — Rediseño frontal guiado

Entrar al frontend solo con el cerebro cerrado: Documento como lector narrado sobrio, rail de capas no invasivo, configuración como bodega técnica y uso obligatorio de componentes transversales.

### T82 — Release Candidate

Empaquetado app-image/MSI, hashes, manifiestos, guías, licencias, FFmpeg/modelos y smoke manual final.
