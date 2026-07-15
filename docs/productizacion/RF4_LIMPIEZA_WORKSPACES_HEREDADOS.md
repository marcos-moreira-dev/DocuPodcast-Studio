# RF4 — Limpieza de workspaces heredados

Estado: implementada sobre RF3 verde.

## Objetivo

Cerrar la cuarentena de workspaces heredados para que Guion interno, Procesos de audio y Secuencia visual no puedan volver como rutas de interfaz principal.

La aplicación mantiene clases y dominio legacy porque siguen sirviendo como compatibilidad interna, payload de proyecto, exportación o transición técnica. La superficie visible del producto sigue siendo:

- Inicio
- Documento
- Voces

## Cambios implementados

- `WorkspaceViewRegistry` valida `WorkspaceSurfacePolicy` al registrar factories.
- El registry rechaza factories para workspaces heredados con mensaje explícito.
- `WorkspaceViewRegistry.viewFor(...)` normaliza solicitudes heredadas con `restoreStartupWorkspace(...)` antes de crear vistas.
- Se eliminaron de `DocuPodcastShellViewModel` los métodos públicos de apertura legacy:
  - `showScriptWorkspace()`
  - `showStoryboardWorkspace()`
  - `showAudioWorkspace()`
- `DocuPodcastShellView` sigue sin registrar handlers para `OPEN_STORYBOARD` ni `OPEN_AUDIO_JOBS`.
- Los comandos legacy de apertura siguen en `AppCommandRegistry` solo como comandos ocultos para compatibilidad/catalogación, no como acciones visibles.

## Alcance

Esta tanda no borra paquetes legacy como `presentation/script`, `presentation/audio` o `presentation/storyboard`. Borrarlos sería una limpieza más agresiva y riesgosa porque aún existen tests, DTOs, exportaciones y compatibilidad interna que dependen de esos tipos.

RF4 sí evita que esas superficies puedan montarse accidentalmente como navegación principal.

## Guardarraíles

Se agrega `LegacyWorkspaceCleanupRf4SourceTest` para validar que:

- el registry rechaza workspaces heredados;
- solicitudes heredadas se enrutan a una superficie de producto;
- el ViewModel no conserva métodos públicos de abrir workspaces legacy;
- el Shell no registra handlers para comandos ocultos de apertura legacy;
- los comandos legacy permanecen ocultos, no visibles.

## Validación esperada

Después de aplicar RF4 se debe ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

En este entorno se validó con `javac` focal y tests fuente con stubs JUnit, pero Maven completo debe ejecutarse en Windows.

## Próxima tanda recomendada

RF5 — Limpieza residual STT/Whisper.
