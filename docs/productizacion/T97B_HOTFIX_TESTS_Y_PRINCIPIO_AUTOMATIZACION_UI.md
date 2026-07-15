# T97B — Hotfix tests y principio de automatización de pasos previos

## Objetivo

T97B corrige dos fallos de tests detectados localmente después de T97 y documenta un principio GUI/producto antes del contrato visual T98.

## Correcciones

- `DocuPodcastProjectFileRepositoryTest` se alinea con el formato vigente: `formatVersion = 2` desde T93.
- `T90G_SMOKE_MODULAR_MOTORES.md` evita reintroducir la etiqueta visible `Audio a texto`; el smoke modular sigue limitado a Coqui/XTTS, Piper y FFmpeg.

## Principio GUI: automatizar pasos previos obvios

Cuando el usuario ejecuta una acción principal y faltan pasos previos obvios, la app debe encadenarlos de forma asistida en vez de bloquear al usuario con precondiciones técnicas.

Ejemplo de producto:

1. El usuario abre un documento Word.
2. El usuario pulsa `Escuchar documento`.
3. Si no existe un proyecto guardado, la app explica: para escuchar se creará un proyecto DocuPodcast con audios, medios y manifiestos.
4. El usuario elige `Aceptar` o `Cancelar`.
5. Si acepta, la app abre selector de ubicación/carpeta y crea la carpeta contenedora del proyecto.
6. La app continúa con proyección narrativa, preparación/generación de audio y reproducción.

## Reglas

- No pedir al usuario que vaya manualmente a otra vista si la acción principal puede preparar lo necesario con confirmación.
- No esconder precondiciones técnicas detrás de mensajes crípticos.
- No crear proyectos o descargar motores sin permiso explícito.
- Sí automatizar preparación de guion interno, proyecto contenedor, importación/normalización de medios y validación de motores cuando el paso sea evidente.
- Si el usuario cancela, la acción se detiene sin ensuciar el estado.

## Impacto para T98

T98 debe aterrizar en piedra:

- MenuBar
- Ribbon
- Workspace
- Sidebar izquierdo
- Rail derecho
- vistas secundarias
- reglas de automatización de pasos intermedios

T97B no rediseña la GUI; solo fija el guardarraíl antes de rediseñar.
