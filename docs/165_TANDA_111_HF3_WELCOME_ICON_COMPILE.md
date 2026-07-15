# Tanda 111-HF3 — Corrección de compilación en iconos de Inicio

Hotfix sobre T111-HF2.

## Motivo

El diagnóstico local `20260602-144025` falló en `Maven compile` por una incompatibilidad en `WelcomeWorkspaceView`:

```text
AppIcon cannot be converted to String
```

La vista de Inicio seguía intentando construir un `Label` con el resultado de `RibbonIconCatalog.iconFor(...)`, pero desde T111-HF2 ese catálogo devuelve `AppIcon`, no texto.

## Corrección

- `WelcomeWorkspaceView` ahora usa el componente transversal `IconView.sideDock(...)`.
- Se conserva `RibbonIconCatalog` como fuente semántica de iconos.
- No se agregan estilos inline ni controles hardcodeados.

## Alcance

Hotfix mínimo de compilación. No cambia reglas de producto ni roadmap.
