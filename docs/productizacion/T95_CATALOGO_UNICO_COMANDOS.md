# T95 — Catálogo único de comandos

## Objetivo

T95 crea el contrato previo al rediseño fuerte de GUI: cada acción de producto debe tener un `AppCommandId` estable, un dueño conceptual y una superficie principal.

La meta es evitar que menú, toolbar, workspace, sidebars y futuro ribbon implementen la misma acción de forma duplicada.

## Alcance implementado

Se agregan clases de presentación sin tocar todavía el rediseño visual fuerte:

- `presentation.command.AppCommandId`
- `presentation.command.AppCommandOwner`
- `presentation.command.AppCommandSurface`
- `presentation.command.AppCommandDescriptor`
- `presentation.command.AppCommandRegistry`
- `presentation.command.AppCommandDispatcher`
- `presentation.command.AppCommandDispatchResult`
- `presentation.command.AppCommandHandler`
- `presentation.command.WorkspaceCapabilityCommandMapper`

También se actualiza `WorkspaceToolbarAction` para transportar `commandId` junto a la `WorkspaceCapability` heredada.

## Reglas de producto

- Si una acción aparece, debe mapear a un comando estable.
- Un comando tiene una superficie visual principal.
- Superficies secundarias pueden invocarlo, pero no deben duplicar lógica.
- No se reintroduce Whisper/STT/Audio a texto como comando visible de producto.
- `Audio del computador` sigue siendo clip genérico elegido por el usuario.

## Ejemplos de dueño visual

- `LISTEN_DOCUMENT`: dueño principal `WORKSPACE_PLAYBAR`.
- `IMPORT_AUDIO_FOR_SELECTION`: dueño principal `LEFT_SIDEBAR`.
- `TOGGLE_FULLSCREEN`: dueño principal `MENU_BAR`/futura pestaña Vista, no toolbar protagonista.
- `OPEN_SOURCE_DOCUMENT`: dueño principal bienvenida/menú/ribbon, con handler único.

## Qué no hace esta tanda

T95 no rediseña todavía menú, ribbon ni sidebars. Prepara el contrato para T96/T97/T98 y para el rediseño posterior.

## Validación

Se agregan tests de registro, dispatcher, mapper y guardarraíles fuente para impedir comandos visibles sin contrato.
