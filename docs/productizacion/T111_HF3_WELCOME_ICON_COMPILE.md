# T111-HF3 — Welcome usa IconView transversal

T111-HF3 corrige la compilación rota tras T111-HF2: `WelcomeWorkspaceView` ya no intenta usar `AppIcon` como `String`. La pantalla Inicio usa `IconView.sideDock(RibbonIconCatalog.iconFor(...))`, manteniendo la regla de componentes transversales para iconografía.

Validación esperada local:

```bat
scripts\99-diagnostico-completo.bat
```
