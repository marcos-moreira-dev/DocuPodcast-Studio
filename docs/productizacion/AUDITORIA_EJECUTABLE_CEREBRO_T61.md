# Auditoría ejecutable del cerebro — Tanda 61

## Propósito

Tanda 61 deja de tratar el **Refrescar contenido** como simple botón de interfaz. La acción pertenece al cerebro de DocuPodcast porque conecta documento fuente, snapshot importado, comparación de cambios y vigencia de artefactos derivados.

La experiencia sigue siendo simple para el usuario:

```text
Abrir documento → leer/escuchar → refrescar contenido si la fuente cambió → revisar audio/capas/storyboard → exportar
```

Pero internamente el cerebro debe distinguir:

- documento fuente externo, siempre solo lectura;
- Documento narrable del proyecto;
- proyección interna de narración;
- audio generado;
- capas narrativas;
- storyboard/video;
- reportes de obsolescencia.

## Corte ejecutable agregado

Se agregan contratos Java mínimos para la comparación de fuente:

```text
SourceDocumentSnapshot
SourceDocumentChangeStatus
SourceDocumentChangeReport
DerivedArtifactFreshness
RefreshSourceDocumentUseCase
RefreshSourceDocumentResult
```

Estos contratos todavía no rediseñan la UI ni conectan el botón final. Son la primera pieza ejecutable del cerebro para evitar que el futuro `Refrescar contenido` se implemente como un handler improvisado en JavaFX.

## Regla de fuente inmutable

El caso de uso de refresco:

1. parte de un `ReadableDocument` ya importado;
2. calcula un snapshot del contenido actual;
3. reimporta el archivo fuente desde disco;
4. compara snapshots;
5. devuelve un `SourceDocumentChangeReport`;
6. no edita ni sobrescribe Word/DOCX, PDF, Markdown/MD o TXT.

Si el contenido cambió, el reporte debe marcar:

```text
audio = obsoleto
capas = en revisión
storyboard = en revisión
```

Si el contenido no cambió, esos artefactos siguen vigentes.

Si el archivo fuente falta, no se reemplaza el documento actual y el usuario debe revisar manualmente.

## Auditoría de deuda del cerebro

Deuda vigente reconocida:

| Área | Estado actual | Dirección |
|---|---|---|
| `DocuPodcastShellViewModel` | queda bajo límite transitorio de 2650 líneas hasta RF-TX1, pero sigue concentrando flujos | seguir bajando mediante coordinadores |
| Navegación/dirty | `withViewState` puede ensuciar por navegación | separar estado visual de contenido |
| Guion | aún existe como workspace/proyección avanzada | no tratarlo como raíz obligatoria |
| Refrescar fuente | contrato ejecutable inicial | conectar a coordinador y UI luego |
| Configuración | mayormente informativa | convertir a settings persistentes |
| Video | paquete renderizable honesto | MP4 real cuando FFmpeg esté cerrado |

## Coordinadores objetivo

La siguiente tanda de refactor debe extraer responsabilidades hacia:

```text
DocumentIntakeCoordinator
NarratedDocumentCoordinator
SourceDocumentRefreshCoordinator
NarrationProjectionCoordinator
NarrativeLayerCoordinator
PlaybackWorkflowCoordinator
AudioWorkflowCoordinator
StoryboardWorkflowCoordinator
SettingsWorkflowCoordinator
VideoExportCoordinator
WorkspaceNavigationCoordinator
ReadingComfortCoordinator
```

## Criterio de no regresión

Toda ampliación del cerebro debe cumplir:

- no aumentar `DocuPodcastShellViewModel` sin registrar deuda;
- no mover lógica de refresh a una vista JavaFX;
- no tratar documentos fuente como editables;
- no prometer que audio/capas/storyboard siguen vigentes si el texto fuente cambió;
- mantener tests unitarios para dominio/application, no solo source tests documentales.
