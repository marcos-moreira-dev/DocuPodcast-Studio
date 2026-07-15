# EXPORT-CLEAN1 — Exportaciones técnicas fuera del flujo común

## Objetivo

Limpiar las superficies principales de exportación para que el usuario normal vea salidas finales de producto:

- Audio final.
- Video MP4 final.
- Estado de exportación.
- Carpeta de exportaciones.

Los paquetes técnicos, reportes de soporte e integridad quedan disponibles, pero se mueven a **Ayuda > Soporte avanzado** para no competir con el flujo principal.

## Cambios

- `AppCommandRegistry` conserva `EXPORT_PROJECT_BUNDLE` y `EXPORT_DIAGNOSTIC_REPORT`, pero los etiqueta como soporte avanzado.
- `Exportar` ya no muestra el paquete técnico del proyecto ni el reporte técnico.
- `Ayuda` agrega submenú `Soporte avanzado` con:
  - Exportar paquete de soporte.
  - Exportar reporte de soporte.
  - Validar integridad del proyecto.
- El Ribbon `Exportar` queda concentrado en audio final, video final, estado y carpeta de exportaciones.
- La toolbar legacy deja de enviar `EXPORT_PROJECT_BUNDLE` como acción primaria de salida y apunta a `EXPORT_PODCAST_WAV`.

## Regla de producto

El paquete auditable sigue existiendo para soporte, trazabilidad y diagnóstico, pero no es salida principal. La salida principal es audio/video final.

## Video de estudio documental

Desde Tanda 93, las opciones de video documental dentro del centro de exportaciones usan controles transversales estilizados y tooltips. La ventana muestra una previsualizacion de frame hipotetico con texto inventado, aplicando color de fondo o imagen, color de texto, fuente y tamano configurados. La previsualizacion no genera audio, imagenes ni assets; solo anticipa la apariencia del frame antes de exportar.

## Guardarraíl

`ExportClean1SupportSurfaceSourceTest` bloquea que `EXPORT_PROJECT_BUNDLE` y `EXPORT_DIAGNOSTIC_REPORT` vuelvan al menú Exportar, toolbar o Ribbon.
