# T103A — Hotfix Inicio desktop y Ribbon compacto

T103A corrige dos hallazgos reportados en prueba visual local sobre T103:

1. El Ribbon ocupaba demasiada altura y se sentia como tarjetas gigantes.
2. La pantalla Inicio parecia una landing page web, no una pantalla de arranque de aplicacion desktop.

## Decisiones

- Mantener el Ribbon como superficie real de T101, pero compactarlo.
- No volver a `MainToolbarView`.
- Mantener Inicio moderno, pero con composicion de aplicacion: acciones a la izquierda, bienvenida/flujo al centro y recientes a la derecha.
- Tomar como referencia estructural la pantalla de inicio de Domain Model Studio, sin copiar su estilo antiguo.

## Cambios

- `RibbonButton` pasa de layout vertical a layout horizontal compacto.
- `RibbonGroup` y `RibbonView` tienen alturas controladas.
- `components/ribbon.css` y `components/actions.css` reducen padding/alto de grupos y botones.
- `WelcomeWorkspaceView` deja de usar hero/preview de producto y se reorganiza como pantalla desktop de inicio.
- `welcome.css` elimina el lenguaje de landing y define columnas compactas.
- `ModernVisualThemeSourceTest` se alinea al Ribbon real y deja de exigir tokens visuales en la toolbar legacy.

## No cambia

- No se modifica el cerebro.
- No se implementa Documento limpio T104.
- No se implementa Playbar flotante T105.
- No se implementan recientes reales; el panel sigue como empty state honesto.

## Validacion esperada

Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

La base debe quedar verde antes de avanzar a T104.
