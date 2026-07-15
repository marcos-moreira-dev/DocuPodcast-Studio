# T99B-HF — Alineación de tests y guardarraíles obsoletos

## Estado

T99B-HF parte de T99B y no cambia comportamiento productivo. Es una tanda de higiene técnica para evitar que tests fuente de UI sigan defendiendo decisiones antiguas mientras el código ya migró a catálogo único de comandos y a superficies de producto actuales.

La tanda existe porque, tras la lectura masiva LM-19/LM-21, se detectó una desalineación peligrosa: varios tests todavía esperaban `new MenuItem(...)` manual, handlers directos o workspaces heredados (`SCRIPT_EDITOR`, `AUDIO_JOBS`, `STORYBOARD`) como superficies contextuales visibles. Esos contratos contradicen T99A/T99B.

## Problema que corrige

Antes de este hotfix, algunos guardarraíles podían fallar aunque el código estuviera siguiendo la dirección correcta:

```text
- MenuBar actual usa commandItem(AppCommandId...), pero tests esperaban new MenuItem(...) literal.
- Toolbar actual despacha comandos, pero tests esperaban handlers directos como shellView::handleToggleFullScreen.
- WorkspaceToolbarActionProvider ya solo expone Inicio, Documento y Voces, pero tests esperaban cases de Guion, Audio Jobs y Storyboard.
- Ayuda/exportaciones/configuración ya están centralizadas por AppCommandId, pero tests seguían defendiendo cableado anterior.
```

Eso ensuciaba la base para T99C porque convertía decisiones correctas del roadmap en aparentes regresiones.

## Decisión

La regla queda ajustada:

```text
Los tests fuente deben proteger el contrato vigente, no snapshots viejos de implementación.
```

Por tanto, un test debe preferir verificar:

```text
commandItem(AppCommandId.X)
shellView.dispatchCommand(AppCommandId.X)
.register(AppCommandId.X, handlerUnico)
```

antes que exigir:

```text
new MenuItem("...")
setOnAction(...)
handler directo por superficie
case de workspace heredado visible
```

## Cambios implementados

Se actualizaron tests fuente de presentación/productización para alinearlos con T99A/T99B:

```text
EditableSettingsAndFrontHonestySourceTest
FrontendConfigurationSurfaceSourceTest
MainMenuNavigationSourceTest
DocumentPrimaryOperationSourceTest
MarkdownImportUiSourceTest
ToolbarRecordingPlaybackSourceTest
MainToolbarIconGroupsSourceTest
ExportUiSourceTest
GuideUiSourceTest
AiResourcesUiSourceTest
ToolbarContextualWorkspaceSourceTest
VisibleActionContractSourceTest
SettingsDialogSourceTest
SimpleVideoExportSourceTest
```

## Contratos que ahora protegen

- MenuBar vigente: `Archivo`, `Proyecto`, `Fuente documental`, `Ver`, `Lectura`, `Exportar`, `Configuración`, `Ayuda`.
- El shell debe usar `AppCommandDispatcher` y `commandItem(AppCommandId...)`.
- El toolbar global/contextual debe despachar por `AppCommandId` y no por handlers directos.
- `WorkspaceToolbarActionProvider` no debe reintroducir `SCRIPT_EDITOR`, `AUDIO_JOBS` ni `STORYBOARD` como cases contextuales visibles.
- Recursos IA, Guía, Configuración y Exportaciones deben verificarse desde el catálogo/dispatcher, no por `new MenuItem(...)` manual.
- STT/Whisper y `Audio a texto` siguen fuera de la UI visible.

## Alcance explícitamente no incluido

T99B-HF no implementa:

```text
- T99C visual mínimo;
- Ribbon real;
- StatusBar con ReadingZoomControl;
- overlay de procesos largos;
- playbar flotante final;
- refactor grande del ShellViewModel;
- preflight real de arranque.
```

Tampoco elimina toda deuda de tests históricos. Solo corrige la desalineación más peligrosa para poder seguir con T99C sin pelear contra guardarraíles obsoletos.

## Validación realizada en entorno ChatGPT

No se ejecutó Maven completo porque `mvn` no está instalado en este entorno.

Validación focal realizada:

```text
- Compilación con javac --release 21 de los 14 tests modificados usando stubs mínimos de JUnit.
- Ejecución reflexiva de los 16 métodos @Test modificados con Assertions funcionales de prueba.
- Revisión fuente de que los tests actualizados pasan contra el código actual T99B-HF.
- Verificación de que no hubo cambios de comportamiento productivo.
```

## Próxima tanda recomendada

T99C — Deshuesadero visual mínimo.

Debe limpiar antes de construir la GUI final:

```text
- Det/Aud/Img truncado;
- emojis como iconografía final;
- textos permanentes de onboarding dentro de la hoja;
- etiquetas Párrafo/Título/Subtítulo dentro del documento;
- restos visuales de scaffold;
- CSS base previo a Ribbon/Sidebar/Rail/Playbar/StatusBar.
```
