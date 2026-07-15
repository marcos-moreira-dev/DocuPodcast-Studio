## Actualización Tanda 9

Tanda 9 implementada: reintentos y diagnóstico de proceso para TTS real. Próxima recomendada: Tanda 10 — Voice Library.

# 23 — Plan de implementación por tandas

Este es el plan maestro de implementación. Cada tanda debe cerrar con código, tests y documentación mínima actualizada.

## Tanda 1 — Onboarding y scaffolding inicial

Ya iniciada. Crea aplicación JavaFX mínima, pantalla de bienvenida, toolbar inicial, CSS claro, bootstrap, docs y scripts.

## Tanda 2 — Proyecto `.docupodcast.json`

Dominio de proyecto, metadata, asset catalog, repository JSON, save/open, rutas relativas, tests de roundtrip.

## Tanda 3 — Importador DOCX

Apache POI, `ReadableDocument`, bloques, títulos/subtítulos, listas, imágenes, alt text, diagnóstico, workspace Documento.

## Tanda 4 — Guion narrable

`NarrationScriptDocument`, segmentos, validación, selección, ScriptWorkspace, SideDock de segmentos/propiedades.

## Tanda 5 — Audio mock

Audio jobs, DTOs, cola, progreso, ETA, cancelación, logs, WAV fake por segmento, UI completa sin TTS real.

## Tanda 6 — TTS real

Integración detrás de `AudioGenerationGateway`: Piper/XTTS worker/otra opción. WAV por segmento, merger, errores.

## Tanda 7 — Reanudación

`job.json`, `segments-status.json`, manifests, reabrir job incompleto, reintentar fallidos.

## Tanda 8 — Voces/personajes/estilos

Biblioteca, voz prediseñada, voz propia, voz autorizada, personaje, estilos como intención, pruebas.

## Tanda 9 — Storyboard vivo

Media assets, imagen por segmento, canvas visual, playback highlight, storyboard package.

## Tanda 10 — Exportaciones

Guion Markdown, WAV, MP3 si encoder, paquete de proyecto, reporte diagnóstico, preview PNG.

## Tanda 11 — Guía y recursos IA

Guía integrada, Word-first, Markdown contracts, plantillas, ejemplos importables, índice recursos IA.

## Tanda 12 — Hardening

Tests fuente, scripts completos, package app-image/MSI, smoke manual, documentación de release.


## Actualización posterior a Tanda 2

Se implementó la base `.docupodcast.json` + assets relativos. Antes del importador DOCX se agrega una tanda intermedia:

```text
Tanda 2.5 — Integración UI de proyecto/session
```

Razón: la capa interna de persistencia ya existe, pero el usuario todavía no puede crear, abrir, guardar ni cerrar proyectos desde la interfaz. Esa sesión es necesaria antes de cargar Word/DOCX en un proyecto real.

Después de Tanda 2.5 continúa:

```text
Tanda 3 — Importador Word/DOCX real
```

## Tanda 3 parcial — DOCX operativo

Adelantada parcialmente: existe importer DOCX mínimo, workspace documental y materialización `source/` + `document/document.json` al guardar. Falta estructura lateral, diagnóstico y perfil de lectura.


## Tanda 3/4 — DOCX diagnóstico y Document Workspace

Se implementó el cierre de la primera versión del importador DOCX y se avanzó el workspace Documento con SideDock mínimo: Estructura, Propiedades y Diagnóstico. El JSON documental materializado ahora incluye resumen, metadatos e issues de importación.


## Tanda 3 cerrada / Tanda 4 avanzada

Tanda 3 queda cerrada en primera versión: DOCX importer con diagnóstico. Tanda 4 queda avanzada: Document Workspace con SideDock inicial, pendiente de extracción de paneles y filtros.


## Actualización de plan — Tanda 3 cerrada y Tanda 4 avanzada

- Tanda 3: importador DOCX mejorado con orden de cuerpo, listas, tablas, imágenes, metadata y diagnósticos.
- Tanda 4: workspace Documento avanzado con estructura lateral, diagnóstico, métricas y selección de bloques.
- Validación parcial: compilación `javac --release 21` de domain/application/infrastructure sin JavaFX.
- Validación completa pendiente en entorno local con Maven Toolchain Temurin 21.


## Actualización Tanda 4 / avance Tanda 5

- Tanda 4 implementada: Document Workspace modular con SideDock reutilizable, filtros y acciones manuales de bloque.
- Tanda 5 avanzada: dominio Reading Profile, perfil académico Word-first y aplicación de reglas sobre bloques.
- Sigue pendiente cerrar Tanda 5 con editor/persistencia de perfiles y puente formal hacia guion narrable.

## Actualización posterior — Tanda 5B implementada

Reading Profile queda cerrado en primera versión: editor visible, persistencia en `.docupodcast.json`, previsualización no destructiva y aplicación al documento importado. La siguiente tanda recomendada es **Tanda 6 — Guion narrable**, donde se construirá `NarrationScriptDocument` desde los bloques ya clasificados.

Tandas pendientes actualizadas:

1. Tanda 6 — Guion narrable.
2. Tanda 7 — Audio job mock con progreso/ETA.
3. Tanda 8 — Gateway TTS real.
4. Tanda 9 — Reanudación de jobs.
5. Tanda 10 — Voice Library.
6. Tanda 11 — Storyboard básico.
7. Tanda 12 — Playback sincronizado.
8. Tanda 13 — Exportaciones.
9. Tanda 14 — Guía integrada + recursos IA.
10. Tanda 15 — Packaging / release candidate.

## Estado tras Tanda 6

Implementado:

```text
Word/DOCX → Documento → Reading Profile → Guion narrable
```

La Tanda 6 agrega `NarrationScriptDocument`, `NarrationSegment`, validación básica, workspace Guion y materialización `script/narration-script.json`.

Tandas pendientes:

```text
Tanda 7   — Audio job mock con progreso/ETA
Tanda 8   — Gateway TTS real
Tanda 9   — Reanudación de jobs
Tanda 10  — Voice Library
Tanda 11  — Storyboard básico
Tanda 12  — Playback sincronizado
Tanda 13  — Exportaciones
Tanda 14  — Guía integrada + recursos IA
Tanda 15  — Packaging / release candidate
```

## Estado tras Tanda 7

Implementada la primera versión de audio job mock con progreso, ETA, cancelación cooperativa, WAVs silenciosos por segmento y manifest mínimo.

Tandas pendientes actualizadas:

```text
Tanda 8   — Gateway TTS real
Tanda 9   — Reanudación de jobs
Tanda 10  — Voice Library
Tanda 11  — Storyboard básico
Tanda 12  — Playback sincronizado
Tanda 13  — Exportaciones
Tanda 14  — Guía integrada + recursos IA
Tanda 15  — Packaging / release candidate
```

---

## Actualización Tanda 7B — Persistencia temprana de jobs de audio

Se implementó persistencia temprana de jobs de audio sobre el mock:

- `AudioJobSnapshot` y `AudioSegmentSnapshot`.
- `AudioJobRepository` + `AudioJobFileRepository`.
- Escritura de `jobs/JOB-*/job.json`.
- Escritura de `jobs/JOB-*/segments-status.json`.
- El `MockAudioGenerationGateway` persiste estado durante la generación.
- El workspace Audio muestra historial persistido.
- Al reabrir proyecto, la UI puede recuperar el último estado persistido sin tratarlo como job activo fantasma.

Queda agregada como recomendación la **Tanda 7C opcional — Recuperación operativa/reintento de jobs persistidos**, antes de conectar TTS real si se desea máxima robustez.

Tandas pendientes actualizadas:

```text
Tanda 7C opcional — Recuperación operativa de jobs persistidos
Tanda 8           — Gateway TTS real
Tanda 9           — Reanudación avanzada de jobs
Tanda 10          — Voice Library
Tanda 11          — Storyboard básico
Tanda 12          — Playback sincronizado
Tanda 13          — Exportaciones
Tanda 14          — Guía integrada + recursos IA
Tanda 15          — Packaging / release candidate
```



## Actualización tras Tanda 7C

Implementada: recuperación operativa de jobs persistidos.

Mapa actualizado:

```text
Tanda 7D opcional — UI placeholders para grabación, voz humana y playback clicable
Tanda 8           — Gateway TTS real
Tanda 9           — Reanudación avanzada de jobs con motor real
Tanda 10          — Voice Library
Tanda 11          — Storyboard básico
Tanda 12          — Playback sincronizado
Tanda 13          — Exportaciones
Tanda 14          — Guía integrada + recursos IA
Tanda 15          — Packaging / release candidate
```

La Tanda 7D es opcional porque ya existen modelos base. Si la prioridad es escuchar audio real, avanzar a Tanda 8. Si la prioridad es dejar la UI preparada para voz humana/grabación/playback desde línea, hacer Tanda 7D.

## Actualización tras Tanda 7D

Implementada: UI e infraestructura preliminar para grabación, voz humana y playback clicable.

```text
Tanda 8           — Gateway TTS real
Tanda 9           — Reanudación avanzada de jobs con motor real
Tanda 10          — Voice Library
Tanda 11          — Storyboard básico
Tanda 12          — Playback sincronizado real
Tanda 13          — Exportaciones
Tanda 14          — Guía integrada + recursos IA
Tanda 15          — Packaging / release candidate
```

La Tanda 7D no reemplaza la Voice Library ni el playback real; solo deja contratos, botones y estados listos para no diseñar el TTS real como único camino de audio.


## Estado actualizado tras Tanda 8

Completado: Gateway TTS real por proceso local configurable.

Pendiente inmediato:

- Tanda 9 — Reanudación avanzada de jobs con motor real y diagnóstico de proceso.
- Tanda 10 — Voice Library.
- Tanda 11 — Storyboard básico.
- Tanda 12 — Playback sincronizado real.
- Tanda 13 — Exportaciones.
- Tanda 14 — Guía integrada + recursos IA.
- Tanda 15 — Packaging / release candidate.


## Estado tras Tanda 10

Implementada Voice Library: dominio, persistencia, workspace y asignación a segmento.

Tandas restantes:

- Tanda 10B opcional — Importación de muestras de voz y assets VOICE_SAMPLE.
- Tanda 11 — Storyboard básico.
- Tanda 12 — Playback sincronizado real.
- Tanda 13 — Exportaciones.
- Tanda 14 — Guía integrada + recursos IA.
- Tanda 15 — Packaging / release candidate.


## Tanda 10B — Implementada

Importación de muestras de voz como assets `VOICE_SAMPLE`, copia a `voices/samples/`, enlace a `VoiceProfile.sampleAssetId`, acciones UI y tests. Los archivos estáticos/personales de audio quedan fuera del repositorio.


### Tanda 11 — Storyboard básico — IMPLEMENTADA

Primera versión: una imagen por segmento, importación a `media/images`, binding manual, workspace Storyboard, `storyboard/storyboard.json`.

Siguiente: Tanda 12 — Playback sincronizado real.


## Actualización tras Tanda 12

Tanda 12 queda implementada: playback sincronizado por segmento mediante `PlaybackManifest`, Java Sound WAV-first y resaltado visual en Guion/Storyboard.

Tandas pendientes:

```text
Tanda 13 — Exportaciones
Tanda 14 — Guía integrada + recursos IA
Tanda 15 — Packaging / release candidate
```

## Actualización tras Tanda 13

Tanda 13 implementada: exportaciones iniciales.

Pendiente:

```text
Tanda 14 — Guía integrada + recursos IA
Tanda 15 — Packaging / release candidate
```


## Actualización tras Tanda 14

Tanda 14 implementada: guía integrada offline + exportación de recursos IA oficiales.

Pendiente:

```text
Tanda 15 — Packaging / release candidate
```

## Hotfix scripts Maven root-safe

Se corrigieron los scripts para que funcionen tanto desde la raíz del repositorio como desde la carpeta `scripts\`. Todos los scripts que ejecutan Maven ahora hacen `pushd` a la raíz y usan `call mvn`, evitando el error `MissingProjectException` cuando se ejecutan desde `scripts\`.

También se añadió `ScriptsRootSafeSourceTest` como guardarraíl.



## Hotfix tests Windows v2

Antes de Tanda 15 se aplicó un hotfix para dejar alineados los tests fuente con la UI actual y corregir rutas multiplataforma. No cambia el orden de implementación: Tanda 15 sigue siendo packaging/release candidate.

## Estado actualizado tras Tanda 15

Tandas funcionales 1 a 15 implementadas. Queda recomendado abrir una Tanda 16 posterior para polish visual/UX, no como requisito del MVP técnico sino como mejora de producto.

### Tanda 16 sugerida — Polish visual / UX

- Toolbar contextual real por workspace.
- Iconografía consistente.
- Statusbar con estado de proyecto/audio/playback.
- Pantalla de inicio más expresiva con azul claro controlado.
- Revisión de textos visibles para eliminar jerga técnica residual.
- Ajuste de tamaños, scrolls y espaciados.
- Smoke visual con capturas.


## Tanda 16 — Polish visual / UX

- Controles visibles de ventana.
- Ventana centrada y ajustada a pantalla.
- Toolbar contextual por workspace.
- Statusbar con retroalimentación más clara.
- Pantalla de inicio con azul suave y tarjetas refinadas.
