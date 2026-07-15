# T114-HF1 — Build verde + no-guion visible mínimo

## Propósito

T114-HF1 es una hotfix conservadora aplicada después de RF1. Su objetivo es recuperar la base verde local y alinear la superficie visible con la decisión de producto: DocuPodcast Studio abre documentos y prepara lectura; **Guion no debe ser una categoría de usuario ni un workspace visible**.

Esta tanda no elimina todavía `domain/script` ni `application/script`, porque audio, render, playback y compatibilidad de proyectos antiguos todavía dependen de `NarrationScriptDocument`. La limpieza profunda queda para las tandas CAT-01 posteriores. En esta hotfix se corrige lo urgente: tests desalineados, navegación legacy y mensajes visibles críticos.

## Cambios realizados

### 1. Source tests RF1 corregidos

Los tests heredados de T92/TI3 seguían buscando lógica de exportación dentro de `DocuPodcastShellViewModel`. RF1 movió esa responsabilidad correctamente a `presentation.shell.workflow.ExportWorkflowCoordinator`.

Se actualizaron:

- `AudioClipPlaybackExportT92SourceTest`
- `StoryboardVideoFromRenderPlanTi3SourceTest`

Ahora validan que:

- `ExportWorkflowCoordinator` contiene `exportPlaybackManifest` para exportación WAV desde manifest;
- `ExportWorkflowCoordinator` construye video desde `RenderUnitPlan` antes del fallback legacy;
- `DocuPodcastShellViewModel` delega en `exportWorkflow` en vez de volver a contener la orquestación.

### 2. El shell deja de activar workspaces legacy

Se eliminan activaciones directas desde el ViewModel hacia:

- `WorkspaceKind.SCRIPT_EDITOR`
- `WorkspaceKind.STORYBOARD`

Cuando un flujo interno prepara lectura, video o visuales, el workspace visible vuelve a:

- `WorkspaceKind.DOCUMENT_READER`

Esto evita que el usuario sea enviado a Guion o Storyboard como superficies principales.

### 3. `activeWorkspace` ya no guarda legacy en los flujos tocados

Los flujos actualizados guardan:

```java
.withViewState("activeWorkspace", WorkspaceKind.DOCUMENT_READER.name())
```

en lugar de guardar `SCRIPT_EDITOR` o `STORYBOARD`.

### 4. Textos visibles urgentes limpiados

Se reemplazan mensajes como:

- “preparar guion”
- “pendiente de guion”
- “Crea un guion”
- “guion narrable”
- “Workspace Guion”

por lenguaje de producto:

- “preparar la lectura”
- “pendiente de preparación”
- “Prepara la lectura del documento”
- “lectura preparada”
- “Documento activo”

### 5. Labels de workspace heredado saneados

`WorkspaceKind.SCRIPT_EDITOR` se conserva por compatibilidad, pero su display name deja de ser “Guion”. La navegación principal sigue limitada por `WorkspaceSurfacePolicy` a:

- Inicio
- Documento
- Voces

### 6. Guardarraíl agregado

Se agrega `ProductCleanNavigationT114Hf1SourceTest` para impedir regresiones básicas:

- el shell no debe activar `SCRIPT_EDITOR` ni `STORYBOARD`;
- no debe guardar esos workspaces como `activeWorkspace` desde los flujos tocados;
- la superficie presentation no debe volver a exponer `SCRIPT_EDITOR("Guion")` como label de producto;
- el ViewModel no debe conservar mensajes visibles de “guion narrable” o “Workspace Guion”.

## Fuera de alcance

T114-HF1 no elimina todavía:

- `application/script`
- `domain/script`
- `infrastructure/script`
- import/export Markdown de guion legacy
- recursos IA `docupodcast-script-v1`
- Whisper/STT
- Storyboard como código interno

Esos puntos quedan para las tandas planificadas:

- T114-HF2 — Retiro total Whisper/STT
- T123-CAT01 — Markdown como documento, no guion
- T124 — Cuarentena legacy workspaces/tests
- T122-C01 — Encapsular Script como PreparedReadingProjection

## Validación esperada

En Windows, ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

Resultado esperado:

- Maven compile OK.
- Maven tests OK.
- Smoke automático cerebro OK.
- Preflight motores sin regresión.

## Nota de entorno

En el entorno de generación de esta tanda no está disponible `mvn`, por lo que la validación completa debe correrse localmente en Windows. Se realizó validación estática de los archivos modificados y de los guardarraíles fuente agregados.
