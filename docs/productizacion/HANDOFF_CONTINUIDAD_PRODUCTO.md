# Handoff de continuidad visual y funcional

## Resumen ejecutivo

DocuPodcast Studio debe convertirse en un lector narrado de documentos Word/DOCX con capas de producción audiovisual. El usuario final debe poder operar el programa con conocimientos básicos de ofimática: abrir documento, escuchar, seleccionar fragmentos, asociar voz/audio/imagen y exportar.

## Flujo que debe guiar toda implementación

```text
Abrir Word/DOCX
→ Documento renderizado como página legible
→ Escuchar documento
→ Resaltado de oración activa
→ Opcional: seleccionar fragmento
→ Asignar voz/audio/emoción/imagen
→ Opcional: exportar podcast/video/evidencia
```

## Pantalla principal

Debe ocultar complejidad. No debe parecer cabina técnica.

Permitido:

```text
Abrir Word/DOCX
Escuchar documento / Reproducir desde aquí
Pausar / reanudar
Mini rail plegable
Estado simple
```

No deseado en pantalla principal:

```text
checksums
líneas de comando
modelos
jobs
manifest
parámetros Whisper
rutas internas
configuración TTS avanzada
```

## Configuración

Debe ser ventana/espacio aparte con sidebar simple y panel derecho. Allí viven:

```text
Lectura / Documento
Reproducción / Buffer
TTS / Voz
STT / Whisper
Audio
Storyboard / Video
Modelos
Almacenamiento
Diagnóstico / Rendimiento
```

## Arquitectura visual

Reutilizar `presentation.components`. Crear nuevos componentes solo cuando exista repetición real o una pieza transversal clara. Evitar una proliferación innecesaria de wrappers visuales.

## Próxima prioridad recomendada

Antes de seguir con motores o packaging, ejecutar una tanda de modernización visual real:

```text
Tanda 48/49 visual — tema moderno, Documento como pantalla real, reducción de scaffolding visible.
```

El proyecto ya tiene suficientes piezas internas; ahora el riesgo principal es que el usuario final siga viendo una aplicación técnica antigua.

## Actualización crítica — Tanda 49 planificada

La exportación de video debe tratarse como capacidad del producto cuando el usuario active storyboard o imágenes. No es opcional como feature prescindible. La salida por defecto debe ser mínimo 2K y debe usar FFmpeg embebido en `tools/ffmpeg` cuando esté disponible.

Antes de seguir con más funcionalidad visible, ejecutar una auditoría de scaffolding visual: clasificar cada elemento como operación principal, configuración, avanzado o infraestructura interna. El Documento debe ser la pantalla operativa principal; Configuración debe ser la bodega técnica.

Roadmap documentado completo:

```text
49 Auditoría scaffolding visual + contrato FFmpeg embebido
50 Modernización visual base
51 Documento como pantalla operativa real
52 Flujo Word → escuchar completo
53 Selección y asignación funcional desde UI
54 Mini rail multimedia real
55 Motor de voz usable
56 Asistente real de modelos
57 Streaming/prebuffer robusto
58 Exportación video simple 2K con FFmpeg embebido
59 Round-trip real de usuario
60 Refactor/cohesión interna
61 Configuración avanzada terminada
62 Smoke real con documentos de prueba
63 Packaging / Release Candidate
```

Toda tanda futura debe agregar o actualizar tests.


## Actualización — Tanda 50 modernización visual base

La siguiente continuidad debe partir de una UI modernizada en tokens y CSS modular. El objetivo visual ya no es únicamente que la app funcione, sino que la pantalla principal comunique producto: abrir un Word, leerlo, escucharlo y operar capas simples. La bodega técnica sigue en configuración/asistentes. Evitar paneles anidados por decoración y evitar botones excesivamente redondeados o vistosos.


## Tanda 51 — Documento como pantalla operativa real

La Tanda 51 corrige los guardarraíles afectados por la modularización CSS de Tanda 50B y avanza la pantalla Documento hacia una operación real de lectura/escucha: el SideDock técnico inicia plegado, el documento recibe más protagonismo y la barra lateral queda como herramienta auxiliar. La pantalla principal debe seguir siendo Documento; configuración y motores siguen siendo bodega técnica.


## Tanda 55 registrada — Motor de voz usable

La experiencia de voz queda orientada a usuario normal: XTTS/Coqui es el motor de alta calidad prioritario, Piper es el respaldo liviano y Mock queda para diagnóstico. La pantalla Documento no debe mostrar nombres de motores ni comandos; todo vive en Configuración.

Pendientes críticos: asistente real de modelos, streaming robusto, FFmpeg embebido con video 2K y modo render, round-trip real, refactor, configuración avanzada, smoke y RC.


## Tanda 56 — Asistente real de modelos

Agrega contratos de carpeta local, verificación de archivos mínimos y reconocimiento de checksum para XTTS/Coqui, Piper y Whisper. Mantiene importación manual obligatoria y evita depender de URLs fijas. Tests: `InspectLocalModelFolderUseCaseTest`, `RealModelAssistantSourceTest`.

## Tanda 57 — Streaming/prebuffer robusto

- Estado: implementada en esta base.
- Contrato: 5 fragmentos iniciales, 10 de lookahead, espera visible y continuidad automática si falta audio.
- Tests: `StreamingPlaybackWindowTest`, `StreamingPlaybackRobustnessSourceTest`.

## Tanda 58 — Video simple 2K + FFmpeg embebido

- Video simple es capacidad del producto cuando el usuario usa storyboard/imágenes.
- Resolución predeterminada: 2K (2560x1440); opciones: 720p, 1080p, 2K y 4K.
- FFmpeg se busca primero como herramienta embebida en `tools/ffmpeg/bin/ffmpeg.exe` y no debe exigir PATH global.
- Durante render, la pantalla operativa entra en modo render con progreso y bloqueo temporal de lectura/edición/nuevas exportaciones.
