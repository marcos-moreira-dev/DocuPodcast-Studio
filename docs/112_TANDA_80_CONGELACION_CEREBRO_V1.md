# Tanda 80 — Congelación del cerebro V1

## Base

Aplicable sobre T80C verde: carpeta contenedora corregida en Windows y entrada flexible de media MP3/WAV/video→audio.

## Objetivo

Congelar el cerebro V1 antes del rediseño frontal. La tanda no busca añadir nuevas funciones visibles, sino fijar una matriz ejecutable de capacidades, límites y decisiones de producto para que T81 no vuelva a mezclar experiencia operativa con bodega técnica.

## Cambios principales

Se agrega el paquete:

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/application/brain
```

Con:

```text
BrainV1CapabilityMatrix
BrainV1Capability
BrainV1CapabilityArea
BrainV1CapabilityStatus
```

La matriz declara capacidades como:

```text
document-intake
listen-document
narrative-layers
media-input
project-integrity
export-readiness
compute-device
brain-smoke
```

Y diferidos V2 como:

```text
rich-word-editor
advanced-video-editor
cloud-collaboration
```

Nota posterior: OCR local para PDF escaneado ya fue promovido a capacidad V1 con limites dentro de `document-intake`; si falla, se conserva el fallback visual.

## Documentación agregada

```text
docs/productizacion/BRAIN_V1_FREEZE_T80.md
docs/productizacion/ROADMAP_POST_T80_CEREBRO_CONGELADO.md
```

Se actualizan:

```text
README.md
AI_HANDOFF.md
VALIDATION.md
docs/productizacion/REGISTRO_TANDAS_DOCUPODCAST.md
```

## Tests agregados

```text
BrainV1CapabilityMatrixTest
BrainV1FreezeSourceTest
```

## Decisión de producto

La experiencia principal posterior debe mostrar un lector narrado sobrio. La complejidad técnica queda en Configuración/Diagnóstico:

```text
Configuración = bodega técnica.
Documento = experiencia operativa clara.
Capas = asignación contextual simple.
```

## Resultado esperado

Después de T80, las siguientes tandas no deben seguir expandiendo el cerebro salvo hotfixes. El camino queda:

```text
Lectura 15 — frontend quirúrgico
T81 — rediseño frontal guiado
T82 — release candidate
```
