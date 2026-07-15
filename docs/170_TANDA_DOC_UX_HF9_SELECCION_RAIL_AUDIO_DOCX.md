# Tanda DOC-UX-HF9 — Selección fina, rail visual, audio sincronizado y DOCX visual

## Objetivo

Ajustar la experiencia del workspace Documento después de la validación de PLAYBACK-HF8R: el audio ya avanza de forma estable, pero la lectura debía sincronizar mejor la oración activa, los sidebars y el rail visual.

## Cambios aplicados

- La selección por oración ya no marca visualmente todo el bloque/párrafo con el borde de selección principal cuando existe `DocumentTextRange` activo.
- El seguimiento de playback usa el cue unitario (`SEG-xxx-Uyyy`) para seleccionar la oración correspondiente en la hoja cuando avanza el audio.
- El rail derecho Visual deja de limitarse a ocho tarjetas y lista todos los fragmentos en el scroll.
- La proyección del rail Visual se reconstruye desde las capas de imagen actuales mediante `buildStoryboardFromImageLayers`, evitando que una imagen recién asociada quede invisible por un storyboard cacheado.
- El sidebar izquierdo Audio sincroniza la etiqueta del origen con el motor activo: `Voz local simple` para Piper, `Voz IA avanzada` para motor avanzado y `Modo de prueba` para mock.
- El modo Piper mantiene ocultas las opciones de tonos/emociones expresivas y explica la limitación con texto humano.
- El importador DOCX conserva todas las imágenes embebidas detectables en `word/media`, incluso cuando algunas no quedan referenciadas explícitamente por `document.xml`.
- Las tablas DOCX agregan metadata de filas, columnas y una vista previa Markdown legible para mostrarse como bloque visual no narrable.
- LaTeX textual se detecta como bloque visual identificado, pero no se renderiza como editor matemático.
- La descarga de Voz IA avanzada agrega headers HTTP básicos y fallback cuando `ATOMIC_MOVE` no está soportado.

## Decisiones de producto

- La unidad visible para leer/escuchar es la oración o fragmento, no el contenedor del párrafo.
- El rail Visual debe mostrar todos los fragmentos y reflejar miniaturas reales cuando una imagen ya fue asociada.
- El sidebar Audio no debe prometer capacidades que el motor activo no tiene.
- Las imágenes/tablas/fórmulas del Word son bloques fuente no narrables por defecto; solo entran a la secuencia visual si el usuario las asocia.
- LaTeX se detecta y se informa; renderizarlo queda fuera del alcance V1.

## Validación focal

- `DocumentUxSyncHf9SourceTest`
- `DocumentAudioEngineSyncHf9SourceTest`
- `DocxVisualImportHf9SourceTest`
- `XttsDownloadRobustnessHf9SourceTest`

Validación ejecutada en entorno ChatGPT:

- `javac --release 21` focal de `DocxDocumentImporter` y dependencias documentales.
- `javac --release 21` focal de `DownloadXttsOfficialModelUseCase`, settings, compute y model setup.
- Compilación de los source tests nuevos con stubs JUnit.
- Ejecución reflexiva de 10 métodos de source tests: OK.
- Prueba focal con `src/main/resources/examples/instinto-creativo/source.docx`: 10 bloques, 1 imagen embebida, 1 imagen con base64.
- Prueba focal con `src/main/resources/examples/caso-contable-cafe-luna/source.docx`: detecta tabla con vista previa Markdown.

## Validación local recomendada

```bat
scripts\99-diagnostico-completo.bat
```

Luego probar manualmente:

1. Abrir `Instinto Creativo Demo` o reimportar `source.docx` del ejemplo.
2. Confirmar que la imagen interna del Word aparece como bloque visual fuente.
3. Asociar una imagen a una oración y confirmar que el rail Visual muestra miniatura real en esa tarjeta.
4. Reproducir el documento y verificar que la oración activa se sincroniza con la hoja y sidebars.
5. Cambiar a Voz local simple/Piper y confirmar que el sidebar Audio ya no muestra `Voz IA avanzada` como origen activo.
6. Probar preparación/descarga de Voz IA avanzada desde Configuración.
