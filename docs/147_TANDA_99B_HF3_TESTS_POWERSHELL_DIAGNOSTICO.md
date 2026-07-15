# Tanda 99B-HF3 — Tests de navegación y PowerShell 5.1

Hotfix posterior a T99B-HF2 basado en diagnóstico local real de Windows.

## Correcciones

- `WorkspaceNavigationCoordinatorTest` queda alineado con el contrato vigente: `AUDIO_JOBS` y `STORYBOARD` se normalizan a `DOCUMENT_READER` y no deben restaurarse como workspaces visibles.
- Los scripts `.ps1` de `scripts/` quedan ASCII-safe para evitar errores de parser en Windows PowerShell 5.1 cuando lee UTF-8 sin BOM.
- `ScriptsRootSafeSourceTest` incorpora guardarraíl para no reintroducir caracteres no ASCII en scripts PowerShell.

## Validación local esperada

```bat
scripts\99-diagnostico-completo.bat
```

Después de esta tanda, los pasos de preflight ya no deberían fallar por parser. Si faltan motores/modelos, los reportes deben indicar preparación pendiente, no sintaxis rota.
