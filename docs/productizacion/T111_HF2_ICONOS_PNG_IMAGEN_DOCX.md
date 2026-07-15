# Productización — T111-HF2 iconos PNG e imagen DOCX

T111-HF2 sustituye la iconografía de glifos por recursos PNG controlados por `AppIcon`/`IconView`. Esto permite mantener una estética más cuidada sin hardcodear botones ni estilos en las vistas.

También refuerza la lectura de imágenes embebidas DOCX para que los visuales fuente puedan renderizarse en Documento mediante `SourceVisualBlockView`.

Validación focal en entorno ChatGPT:

- source checks de los dos tests fallidos en `20260602-141546`.
- import focal de `Instinto Creativo.docx`: 10 bloques, 1 `IMAGE_NOTICE`, `embeddedImageBase64` presente.
- ZIP íntegro.

Maven completo debe ejecutarse localmente con `scripts\\99-diagnostico-completo.bat`.
