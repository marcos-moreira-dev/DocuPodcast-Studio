# T110R — Reconstrucción de T110 sobre T109

Base: T109 física (`DocuPodcast-Studio-tanda109-overlay-procesos-largos-v1`).

Motivo: T110 fue implementada en una ventana anterior, pero no quedó disponible como ZIP descargable. Esta tanda reconstruye el contrato funcional de T110 sobre la base T109 y deja explícitas las decisiones nuevas sobre bloques visuales del documento.

## Cambios principales

- `GuideDialog` deja de usar un visor crudo de Markdown y renderiza los temas como nodos JavaFX: títulos, párrafos, listas y bloques de código.
- Los topics visibles de la guía se reescriben para explicar DocuPodcast Studio real: fuente documental, proyecto, lectura, voces, imágenes, audio, storyboard, exportación y solución de problemas.
- Se elimina Whisper/STT/Audio a texto de la guía visible; la infraestructura histórica queda fuera del producto core visible.
- `DocxDocumentImporter` conserva imágenes embebidas de `word/media/*` como metadata `embeddedImageBase64`, `embeddedImageMimeType` y `embeddedImagePath` en bloques `IMAGE_NOTICE`.
- `DocumentWorkspaceView` renderiza la imagen embebida dentro de la hoja de lectura cuando el bloque trae `embeddedImageBase64`.
- Tablas e imágenes DOCX se marcan como bloques visuales fuente (`visualBlock=true`) y con asignación de storyboard controlada por el usuario (`storyboardAssignment=user-controlled`).
- Configuración agrega `video.silentVisualBlockSeconds` con valor por defecto 5.0 segundos para bloques visuales no narrables.
- La playbar solicita cancelación/pausa segura del job de audio activo al pausar, detener o refrescar contenido.
- Reanudar intenta continuar un job recuperable cuando no hay manifest de playback listo.
- MenuBar/Ribbon usan flujo guiado de guardado antes de escuchar, reproducir selección o generar audio si el proyecto aún no tiene archivo `.docupodcast.json`.

## Regla de producto fijada

Imagen, tabla o fórmula/LaTeX detectada en la fuente documental es un bloque visual fuente. No es una asignación automática de storyboard.

Para video/storyboard:

- Fragmento sin visual asignado: se omite.
- Fragmento con visual asignado y audio: se renderiza con audio.
- Bloque visual sin texto narrable pero con visual asignado: se renderiza en silencio.
- Duración silenciosa inicial: 5 segundos, configurable en Configuración.

## Límites conscientes

- `IMAGE_NOTICE` y `TABLE_NOTICE` siguen usando los tipos de bloque existentes para no forzar migración de formato en esta tanda.
- El render end-to-end de bloques visuales silenciosos queda para una tanda posterior basada en `RenderUnit`.
- LaTeX/fórmulas queda documentado como capacidad futura; no se implementa render visual LaTeX real en esta tanda.
- No se hace refactor grande de `DocuPodcastShellViewModel` todavía.

## Validación ChatGPT

- Verificación focal de cambios fuente.
- Prueba focal de `DocxDocumentImporter` con `Instinto Creativo(1).docx`: se detecta imagen embebida y metadata base64.
- Javac focal de clases no-JavaFX modificadas donde el entorno lo permite.
- Source tests focales con stubs JUnit.
- ZIP íntegro.

## Validación local obligatoria

```bat
scripts\99-diagnostico-completo.bat
```

Esperado: Maven compile/tests/smoke/preflight verde. Si quedan fallos, revisar primero source tests relacionados con guía, rail, imagen embebida y configuración.
