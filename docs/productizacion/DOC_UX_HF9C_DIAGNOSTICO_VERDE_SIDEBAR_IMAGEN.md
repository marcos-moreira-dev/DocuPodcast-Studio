# DOC-UX-HF9C — diagnóstico verde y sincronización final de imagen

## Alcance

Hotfix focal posterior a DOC-UX-HF9B. Corrige fallos de guardarraíles del diagnóstico completo y alinea el panel Imagen izquierdo con el mismo resolver de fragmentos que usa el rail Visual derecho.

## Cambios funcionales

- `DocuPodcastShellViewModel.selectedDocumentImageUri()` ahora consulta primero `documentFragmentRailPresentations()` y toma la miniatura del fragmento seleccionado, igual que el rail derecho.
- El panel izquierdo de Imagen deja de resolver la asociación por una ruta distinta cuando la frase ya tiene imagen en el rail Visual.
- Si el rail derecho muestra miniatura para una frase seleccionada, el panel izquierdo debe mostrar la misma imagen.
- El título interno del rail vuelve a `Fragmentos visuales`, conservando la regla nueva de HF9B: una tarjeta por frase/fragmento, tenga o no imagen.

## Correcciones de diagnóstico

- `PreviewReadingProfileUseCaseTest` se actualiza al contrato vigente: imágenes/tablas/fórmulas fuente permanecen como bloques visuales no narrables y no se degradan a `IGNORED`.
- Los source tests del rail se alinean al nuevo flujo por oración: clic en tarjeta usa `selectDocumentTextRange(...)`.
- `ResidualWhisperSttCleanupRf5SourceTest` ignora archivos binarios al inspeccionar scripts/fuentes como texto.
- El guardarraíl de storyboard como capa verifica `bindingsByImageAssetId` en `DocumentRailProjectionFactory`, donde ahora vive la proyección del rail.

## Guardarraíl nuevo

- `DocUxHf9CImageSidebarSyncSourceTest`: protege que el panel izquierdo use el mismo resolver por fragmento que el rail derecho.

## Validación esperada

Ejecutar localmente:

```bat
scripts\99-diagnostico-completo.bat
```

Luego validar manualmente:

1. Abrir un proyecto con una imagen asociada a una frase.
2. Seleccionar esa frase desde el documento.
3. Confirmar que el rail derecho muestra miniatura.
4. Confirmar que el panel izquierdo Imagen muestra esa misma miniatura.
5. Seleccionar la tarjeta desde el rail derecho y repetir la validación.
