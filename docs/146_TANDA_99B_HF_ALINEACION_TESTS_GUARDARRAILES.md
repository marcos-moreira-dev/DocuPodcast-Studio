# Tanda 99B-HF — Alineación de tests y guardarraíles obsoletos

T99B-HF es un hotfix técnico sobre T99B. No cambia comportamiento productivo ni rediseña la GUI. Corrige tests fuente que seguían defendiendo contratos antiguos después de la migración a `AppCommandId`, `AppCommandDispatcher` y superficies productivas Inicio/Documento/Voces.

## Cambios principales

- `MainMenuNavigationSourceTest` queda alineado al menú vigente: Archivo, Proyecto, Fuente documental, Ver, Lectura, Exportar, Configuración y Ayuda.
- `ToolbarContextualWorkspaceSourceTest` deja de exigir `SCRIPT_EDITOR`, `AUDIO_JOBS` y `STORYBOARD` como cases visibles en `WorkspaceToolbarActionProvider`.
- `MainToolbarIconGroupsSourceTest` ya no espera handlers directos como `shellView::handleToggleFullScreen`; exige despacho por `AppCommandId`.
- Tests de Exportar, Guía, Recursos IA y Configuración se actualizan para verificar `commandItem(AppCommandId...)`, registros del dispatcher y catálogo de comandos.
- Tests de lectura/playback/documento se ajustan para no exigir strings legacy incompatibles con T99B.

## Validación en entorno ChatGPT

- Compilación focal con `javac --release 21` de los tests modificados usando stubs mínimos de JUnit.
- Ejecución reflexiva de 16 métodos `@Test` modificados con assertions funcionales.
- No se ejecutó Maven completo porque `mvn` no está disponible en este entorno.

## Próxima tanda

T99C — Deshuesadero visual mínimo.
