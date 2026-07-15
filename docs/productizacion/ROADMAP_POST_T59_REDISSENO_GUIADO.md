# Roadmap post T59 — rediseño guiado por principios rectores

## Estado de entrada

T58B dejó la base verde. T58C agregó el smoke exploratorio mínimo. El log local de usuario confirmó tests verdes en T58C y, al abrir la app, reveló advertencias CSS de tokens/valores visuales que deben tratarse como señal de deuda de scaffolding visual.

## Orden recomendado

| Orden | Tanda | Objetivo |
|---:|---|---|
| 1 | T59A — Componentes GUI transversales | Crear/usar componentes compartidos para acciones, botones, rails, cards y badges. |
| 2 | T59B — Limpieza UX Documento | Quitar métricas técnicas de la pantalla principal y reforzar experiencia lector Word narrado. |
| 3 | T60 — Refactor coordinadores | Reducir `DocuPodcastShellViewModel` después de fijar comportamiento visible. |
| 4 | T61 — Round-trip real | Guardar/cerrar/reabrir con documento, guion, capas, imágenes, voces, audio y storyboard. |
| 5 | T62 — Configuración operativa | Persistir y probar motor TTS, STT, FFmpeg, buffer, rutas y modelos. |
| 6 | T63 — Smoke real completo | Evidencia manual con documentos simples, largos, técnicos, teatrales, audio, STT y video. |
| 7 | T64 — Packaging/RC | App-image/MSI, hashes, manifiestos, guías y limitaciones conocidas. |

## Bloqueos antes de RC

- No debe haber tokens CSS huérfanos.
- No debe haber acciones visibles que prometan MP4 final si solo existe paquete renderizable.
- No debe haber botones hardcodeados en workspaces para patrones ya cubiertos por componentes transversales.
- No debe haber cambios de workspace que ensucien el proyecto como cambio de contenido.
- No debe haber placeholders persistentes tratados como voces/assets reales.


## Actualización T59A — primer contrato ejecutable

T59A ya materializa el principio de componentes GUI transversales. El siguiente paso ya no debe ser crear más controles sueltos, sino simplificar Documento usando el catálogo compartido:

```text
T59B — limpieza UX Documento
T60 — refactor coordinadores/ShellViewModel
T61 — round-trip real
```
