# Tanda 99B — Deshuesadero de acciones duplicadas

T99B continúa el bloque de limpieza GUI iniciado en T99A. La tanda conecta Shell/MenuBar/Toolbar/Inicio al catálogo único de comandos para evitar que una misma acción se implemente varias veces en distintas superficies.

## Cambios principales

- `DocuPodcastShellView` registra un `AppCommandDispatcher` con handlers únicos.
- El MenuBar usa `commandItem(AppCommandId...)` para acciones visibles.
- La vista Inicio invoca `OPEN_SOURCE_DOCUMENT`, `OPEN_PROJECT` y `NEW_PROJECT` mediante dispatcher.
- `MainToolbarView` deja de tener un `switch` de handlers por capability y despacha `action.commandId()`.
- Se agregan `EXIT_APPLICATION`, `CLEAR_SELECTION` y `OPEN_ABOUT` al catálogo.
- El MenuBar temporal se acerca al contrato T98A: Archivo, Proyecto, Fuente documental, Ver, Lectura, Exportar, Configuración, Ayuda.

## Documentación

- `docs/productizacion/T99B_DESHUESADERO_ACCIONES_DUPLICADAS.md`
- `docs/productizacion/T99_GUI_ROADMAP_IMPLEMENTACION_DETALLADO.md`

## Validación en entorno ChatGPT

- ZIP íntegro.
- Compilación focal de `presentation.command` con `javac --release 21`.
- Verificación fuente de Shell/MenuBar/Toolbar/Welcome.
- No se ejecutó Maven completo porque `mvn` no está disponible en este entorno.

## Próxima tanda

T99C — Deshuesadero visual mínimo.
