# Tanda 100 — MenuBar final

T100 cierra el MenuBar como superficie estable de comandos antes de construir el Ribbon.

## Cambios

- Nuevos comandos:
  - `OPEN_SOURCE_DOCUMENT_LOCATION`
  - `OPEN_EXPORTS_FOLDER`
  - `TOGGLE_RIGHT_RAIL`
- Menú Fuente documental incorpora abrir ubicación de fuente.
- Menú Ver incorpora mostrar/ocultar rail derecho.
- Menú Exportar incorpora estado de exportación y apertura de carpeta `exports/`.
- El rail derecho queda controlado por estado real en `DocuPodcastShellViewModel` y se enlaza desde `DocumentWorkspaceView`.
- La exportación de video simple deja de reetiquetarse como storyboard desde la vista.

## Fuera de alcance

- Ribbon real.
- StatusBar con zoom.
- Overlay de procesos largos.
- Refactor grande del ShellViewModel.

## Validación sugerida

```bat
scripts\99-diagnostico-completo.bat
```
