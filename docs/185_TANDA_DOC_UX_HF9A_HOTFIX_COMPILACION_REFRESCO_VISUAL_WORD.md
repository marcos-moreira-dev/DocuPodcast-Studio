# Tanda DOC-UX-HF9A — Hotfix compilación y refresco visual Word

## Base

Aplicada sobre `DOC-UX-HF9 — selección fina, rail visual, audio sincronizado y DOCX visual`.

## Cambios

1. Corrige el fallo local de Maven:
   - `DocuPodcastShellViewModel.java:[1655,32] local variables referenced from a lambda expression must be final or effectively final`.
   - Se introduce `storyboardForImageLayers` como referencia final antes de la lambda.

2. Refuerza el refresco de imágenes/tablas internas del Word:
   - `SourceDocumentRefreshCoordinator` compara metadata de bloques visuales fuente.
   - Si el texto no cambió pero se rehidrataron visuales internos, marca el documento como actualizado sin invalidar audio.
   - La barra de estado explica que se debe guardar el proyecto para conservar esos visuales.

3. Agrega guardarraíl:
   - `DocUxHf9AHotfixSourceTest`.

## Prueba recomendada

```bat
scripts\99-diagnostico-completo.bat
```

Luego, para un proyecto demo antiguo de Instinto Creativo:

1. Abrir el proyecto.
2. Usar `Fuente documental > Refrescar contenido` o el botón `Refrescar contenido`.
3. Confirmar que la imagen interna de Word se rehidrata si existe.
4. Guardar el proyecto para persistirla en `document/document.json`.

