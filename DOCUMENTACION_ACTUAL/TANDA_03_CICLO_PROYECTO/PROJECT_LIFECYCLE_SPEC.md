# Tanda 3 - Ciclo de vida de proyectos y fuentes

Fecha de implementacion: 2026-06-24

## Resumen

Tanda 3 formaliza el ciclo funcional de proyecto y fuente primaria. El producto conserva `ProjectMode` como modalidad oficial y mantiene una sola fuente primaria documental cuando el proyecto trabaja desde documento o gramatica. Los proyectos sin fuente y la escucha rapida siguen existiendo, pero como estados explicitos.

No se modifica el schema JSON, no se cambia `ProjectMode`, no se elimina `ProjectKind` y no se toca el respaldo.

## Estados operativos

- Sin proyecto: el usuario puede abrir un proyecto, crear uno nuevo o crear desde documento/fuente.
- Proyecto nuevo sin fuente: sesion dirty, sin archivo `.docupodcast.json`, workspace `WELCOME_HOME`, ribbon gobernado por el modo elegido.
- Proyecto con fuente externa importada: sesion dirty, workspace `DOCUMENT_READER`, fuente primaria pendiente de materializar si el proyecto aun no tiene ubicacion.
- Proyecto guardado: fuente primaria canonica dentro de `source/`, documento hidratado desde la copia del proyecto y aviso de fuente canonica disponible.
- Proyecto antiguo o sin fuente formal: abre sin migracion destructiva; queda como proyecto sin fuente primaria hasta que el usuario asocie una.

## Transiciones implementadas

### Nuevo proyecto

El flujo ahora tiene dos decisiones separadas:

1. `ProjectNameDialog` pide nombre y modalidad oficial.
2. `ProjectInitialSourceDialog` pide elegir fuente ahora, crear sin fuente o cancelar.

Si el usuario elige fuente, el shell abre el selector de documento y no crea la sesion hasta que la importacion/clasificacion termina correctamente. Si el usuario cancela el selector, no queda una sesion parcial. Si elige crear sin fuente, se crea el proyecto dirty sin documento.

### Crear desde documento / abrir fuente

`OPEN_SOURCE_DOCUMENT` sigue siendo el comando visible actual. Cuando no hay fuente activa, importar un documento conserva el flujo de escucha rapida: si no existe proyecto, `attachImportedDocument` crea uno desde el titulo de la fuente.

Cuando hay fuente activa, el mismo comando se interpreta como reemplazo de fuente y exige confirmacion antes de importar.

### Reemplazar fuente

`ProjectSourceReplacementDialog` confirma que se va a reemplazar la fuente primaria y que lectura preparada, storyboard y audio derivado dejan de ser validos para la nueva fuente.

Si el proyecto tiene cambios sin guardar, se aplica `UnsavedChangesDialog` antes de importar:

- Guardar: guarda primero; si el guardado se cancela o falla, no se reemplaza.
- Descartar: si el proyecto tiene archivo, recarga primero ese archivo para descartar cambios no guardados; si aun no tiene archivo, continua con la sesion en memoria porque no existe estado guardado al cual volver.
- Cancelar: no cambia nada.

La misma puerta se aplica a importacion documental normal, gramatica teatral y gramatica narrativa, porque las tres rutas cambian la fuente primaria.

### Guardar y fuente canonica

El guardado sigue usando `ProjectContainerPathPolicy`. Al guardar un proyecto con documento importado, `ProjectWorkflowCoordinator` materializa la fuente en `source/` y rehidrata la sesion con el documento canonico del proyecto.

### Cerrar, abrir otro proyecto y dirty state

Cerrar proyecto, abrir proyecto y crear proyecto nuevo siguen pasando por `confirmDiscardOrSaveIfNeeded`. No se agrego una nueva regla de persistencia: la proteccion se concentra en la transicion de UI antes de reemplazar sesion o fuente.

## Reglas de fuente primaria

- Fuente primaria es el documento o gramatica que origina el lector actual.
- Imagenes, voces, assets de objeto/personaje, audio jobs y anexos no son segunda fuente primaria.
- Reemplazar fuente invalida derivados de lectura: guion preparado y storyboard se limpian en `ProjectSession`.
- El nombre explicito del proyecto se conserva al asociar una fuente; crear desde documento sin proyecto sigue usando el titulo de la fuente como titulo inicial.

## Impacto en workspace y ribbon

La tanda no rediseña el ribbon. El impacto queda en el estado que consume el ribbon:

- `ProjectMode` sigue marcando modalidad real.
- `ProjectModeCapabilities` sigue controlando comandos visibles.
- Asociar fuente activa `DOCUMENT_READER` mediante view state.
- Proyectos sin fuente mantienen el workspace de inicio hasta que el usuario asocie documento o navegue a una vista disponible.

## Migracion de proyectos antiguos

No hay migracion de schema. Un proyecto antiguo sin fuente formal abre como proyecto sin fuente primaria. Cuando el usuario importe o reemplace fuente, la siguiente operacion de guardado materializa la copia canonica en `source/`.

## Validacion ejecutada

Pruebas enfocadas nuevas y contrato de modos:

```powershell
mvn -q "-Dtest=DocumentIntakeCoordinatorTest,ProjectLifecycleTanda3SourceTest,OfficialProjectModesTanda3SourceTest" test
```

Resultado: pasa.

Suite minima ampliada de Tanda 3:

```powershell
mvn -q "-Dtest=ProjectSessionCoordinatorTest,ProjectWorkflowCoordinatorSourceTest,ProjectRoundTripUseCaseTest,LoadProjectWorkspaceArtifactsUseCaseTest,ValidateProjectWorkspaceIntegrityUseCaseTest,RefreshSourceDocumentUseCaseTest,ProjectAssetReferenceTest,ProjectContainerPathPolicyTest,DocuPodcastProjectFileRepositoryTest,OfficialProjectModesTanda3SourceTest,DocumentIntakeCoordinatorTest,ProjectLifecycleTanda3SourceTest" test
```

Resultado: pasa.

No se ejecutaron builds ni tests en el respaldo.
