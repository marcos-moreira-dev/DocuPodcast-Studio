# T99B — Deshuesadero de acciones duplicadas

## Estado

T99B parte de T99A y continúa el bloque de limpieza GUI antes del rediseño fuerte. La tanda no intenta crear todavía el Ribbon final ni rediseñar la hoja. Su objetivo es impedir que MenuBar, toolbar, pantalla de inicio y futuras superficies implementen la misma acción con lógica distinta.

## Problema que corrige

Antes de esta tanda había varias superficies que invocaban handlers directamente:

```text
MenuBar      -> viewModel / shell handlers directos
Toolbar      -> switch por WorkspaceCapability
Inicio       -> callbacks directos a handlers
Sidebar      -> todavía conserva lógica contextual propia
```

Eso produce riesgo de:

```text
- duplicar lógica por botón;
- reintroducir placeholders;
- tener una acción con dos comportamientos;
- hacer que el futuro Ribbon copie handlers viejos;
- trabajar encima de decisiones alucinadas de UI.
```

## Decisión de producto

La regla queda formalizada:

```text
Una acción de usuario = un AppCommandId.
Las superficies visuales solo invocan comandos.
La lógica real vive en un único handler registrado.
```

Si una acción aparece en más de una superficie, por ejemplo `Escuchar documento` en MenuBar, Ribbon futuro y playbar, todas deben invocar el mismo comando:

```text
AppCommandId.LISTEN_DOCUMENT
```

## Cambios implementados

### 1. Shell con dispatcher transversal

`DocuPodcastShellView` ahora tiene:

```text
AppCommandRegistry commandRegistry
AppCommandDispatcher commandDispatcher
registerCommandHandlers()
dispatchCommand(AppCommandId)
commandItem(AppCommandId)
```

Esto deja el shell como puente inicial entre el catálogo de comandos y los handlers existentes.

### 2. Welcome usa comandos

La vista de inicio ya no recibe callbacks directos a `handleImportWord`, `handleOpenProject` o `handleNewProject`. Ahora invoca:

```text
OPEN_SOURCE_DOCUMENT
OPEN_PROJECT
NEW_PROJECT
```

### 3. MenuBar usa `commandItem(...)`

El MenuBar deja de cablear la mayoría de acciones visibles con `setOnAction(...)` manual a ViewModel/shell handlers. En su lugar usa `commandItem(AppCommandId.X)`.

Ejemplos:

```text
OPEN_SOURCE_DOCUMENT
SAVE_PROJECT
LISTEN_DOCUMENT
EXPORT_PROJECT_BUNDLE
EXPORT_PODCAST_WAV
TOGGLE_FULLSCREEN
OPEN_SETTINGS
OPEN_GUIDE
```

### 4. Toolbar usa `AppCommandId`

`MainToolbarView` ya no tiene un `switch` que implementa handlers por `WorkspaceCapability`. Usa `WorkspaceToolbarAction.commandId()` y llama:

```text
shellView.dispatchCommand(action.commandId())
```

El toolbar global también invoca comandos para:

```text
SHOW_WELCOME
OPEN_SOURCE_DOCUMENT
LISTEN_DOCUMENT
EXPORT_PROJECT_BUNDLE
TOGGLE_FULLSCREEN
```

### 5. Nuevos comandos de soporte

Se agregan comandos que estaban visibles pero no catalogados:

```text
EXIT_APPLICATION
CLEAR_SELECTION
OPEN_ABOUT
```

## Alcance conservador

T99B no rediseña todavía:

```text
- Ribbon final;
- Sidebar izquierdo final;
- Rail derecho final;
- playbar flotante final;
- status bar / zoom final.
```

Tampoco pretende resolver todos los comandos contextuales del sidebar, porque esa superficie se rediseñará en T106. La meta es dejar el patrón listo y eliminar duplicaciones más peligrosas en Shell/Menu/Toolbar/Inicio.

## Menú temporal resultante

El MenuBar ya se acerca al contrato T98A:

```text
Archivo
Proyecto
Fuente documental
Ver
Lectura
Exportar
Configuración
Ayuda
```

Aún no es la implementación visual final de T100, pero ya usa comandos y reduce duplicación.

## Guardarraíles

Se agrega `CommandDispatchCleanupT99BSourceTest`, que verifica:

```text
- Shell tiene AppCommandDispatcher.
- Shell registra handlers por comando.
- MenuBar usa commandItem(AppCommandId...).
- Toolbar global usa dispatchCommand.
- Toolbar contextual usa action.commandId().
- Welcome invoca comandos y no handlers directos.
```

## Criterios de aceptación

```text
- No hay handlers duplicados en MenuBar para acciones catalogadas.
- El toolbar contextual ya no implementa un switch de handlers.
- Las acciones principales pasan por AppCommandDispatcher.
- Whisper/STT no vuelve al catálogo visible.
- Las tandas futuras de Ribbon/Menu/Sidebar deben usar AppCommandId.
```

## Próxima tanda

T99C — Deshuesadero visual mínimo.

Debe limpiar antes del rediseño fuerte:

```text
- Det/Aud/Img truncado;
- emojis como iconografía final;
- textos de relleno visibles;
- etiquetas Párrafo/Título/Subtítulo dentro de la hoja;
- restos visuales de scaffolding;
- preparación CSS para Ribbon/Sidebar/Rail/Playbar.
```
