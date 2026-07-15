# Tanda correctiva — XTTS-HF2 + AUDIO-ENGINE-CATALOG-HF1

Esta tanda corrige los fallos locales reportados después de `XTTS-MODEL-PATH-HF2-v1` y avanza con el catálogo de motores usables.

## Corrección

Los tests locales fallaban porque la política normalizaba cualquier carpeta con posible subruta `tts/xtts` como si fuera raíz de modelos, incluso cuando el usuario había seleccionado una carpeta concreta de XTTS. Ahora solo se agrega `tts/xtts` cuando la ruta parece raíz `models` o contiene una carpeta `tts` real.

## Nueva capacidad

`ListAudioEngineAvailabilityUseCase` entrega una lista de fuentes operativas para Documento. El panel Audio filtra por `usableInDocument` y evita mostrar Voz IA avanzada si no pasó la prueba real necesaria para generar chunks.

## Guardarraíles

- `XttsModelPathPolicyTest` vuelve a respetar carpetas concretas.
- `PortableModelsTransplantSourceTest` conserva la preferencia por `models/tts/xtts` trasplantado.
- `AudioEngineCatalogHf1SourceTest` protege que Documento use el catálogo operativo.
