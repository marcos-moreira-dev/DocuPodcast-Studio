# Tanda 99A — Deshuesadero GUI y roadmap de implementación completo

## Resumen

T99A inicia el bloque de implementación GUI posterior al contrato T98. La tanda limpia navegación vieja antes de construir el MenuBar, Ribbon, Sidebar, Rail, Playbar y workspace final.

## Cambios principales

- Inicio, Documento y Voces quedan como workspaces reales de producto.
- Guion, Audio Jobs y Storyboard dejan de ser workspaces de navegación normal.
- Las rutas persistidas antiguas hacia Guion/Audio/Storyboard vuelven a Documento.
- Voces se conserva como workspace secundario real.
- Se retiran accesos visibles a Narración interna, Jobs de audio y Storyboard como vistas.
- Se agregan documentos detallados para todas las tandas GUI planificadas T99B–T112.

## Documentos agregados

```text
docs/productizacion/T99A_DESHUESADERO_VISTAS_NAVEGACION.md
docs/productizacion/T99_GUI_ROADMAP_IMPLEMENTACION_DETALLADO.md
docs/productizacion/T99B_DESHUESADERO_ACCIONES_DUPLICADAS.md
docs/productizacion/T99C_DESHUESADERO_VISUAL_MINIMO.md
docs/productizacion/T100_MENUBAR_FINAL.md
docs/productizacion/T101_RIBBON_BASE.md
docs/productizacion/T102_STATUSBAR_READING_ZOOM.md
docs/productizacion/T103_VISTA_INICIO_PROPAGANDISTICA.md
docs/productizacion/T104_WORKSPACE_DOCUMENTO_LIMPIO.md
docs/productizacion/T105_PLAYBAR_FLOTANTE.md
docs/productizacion/T106_SIDEBAR_IZQUIERDO_CONTEXTUAL.md
docs/productizacion/T107_RAIL_DERECHO_RETRACTIL.md
docs/productizacion/T108_WORKSPACE_VOCES.md
docs/productizacion/T109_OVERLAY_PROCESOS_LARGOS.md
docs/productizacion/T110_CONFIGURACION_GUIA_DIALOGOS.md
docs/productizacion/T111_ICONOGRAFIA_CSS_FINAL.md
docs/productizacion/T112_SMOKE_VISUAL_CHECKLIST_UX.md
```

## Validación esperada

La suite normal debe seguir verde con:

```bat
scripts\02-ejecutar-tests.bat
```

## Próxima tanda

T99B — Deshuesadero de acciones duplicadas.
