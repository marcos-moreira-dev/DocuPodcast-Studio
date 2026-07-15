# T126A — Fuente documental portable y refresco desde copia del proyecto

## Objetivo

Cerrar una brecha delicada de portabilidad: cuando el usuario guarda un proyecto, el documento Word/PDF/Markdown/TXT importado debe copiarse dentro de la carpeta del proyecto y, desde ese momento, esa copia interna debe ser la fuente canónica para refrescar contenido.

Esto evita que un proyecto enviado como ZIP quede corrupto porque dependía de un Word externo en el Escritorio del usuario original.

## Decisión de producto

El archivo externo original solo sirve como entrada inicial. Después de guardar el proyecto, DocuPodcast trabaja con:

```text
<carpeta-proyecto>/source/<documento-original>
```

La ruta original externa puede quedar como referencia histórica/auditoría en una tanda futura, pero no debe ser la ruta operativa para refrescar.

## Escenario que motivó la tanda

1. El usuario tiene `poesia.docx` en el Escritorio.
2. Abre ese Word en DocuPodcast.
3. Guarda un proyecto en el Escritorio.
4. Quiere mandar el proyecto como ZIP a otra persona.
5. Si el proyecto dependiera de `C:\Users\...\Desktop\poesia.docx`, en la otra computadora fallaría.

Con esta tanda, el proyecto queda autocontenido porque contiene su propia copia:

```text
MiPoesia.docupodcast/
  MiPoesia.docupodcast.json
  source/
    poesia.docx
  document/
    document.json
  jobs/
  assets/
  voices/
```

## Comportamiento implementado

- `ReadableDocumentWorkspaceRepository.materialize(...)` copia el documento fuente a `source/<archivo>`.
- Si el documento ya apunta a la copia interna, no se copia a sí mismo.
- La materialización devuelve `projectSourceDocument`, un `ReadableDocument` rebasado a la copia interna del proyecto.
- `MaterializeImportedDocumentUseCase.materializeWithResult(...)` devuelve el proyecto actualizado más el documento canónico.
- `ProjectWorkflowCoordinator.saveProject(...)` actualiza la sesión con el documento canónico.
- `DocuPodcastShellViewModel.saveCurrentProjectAs(...)` actualiza `currentDocument` desde la sesión después de guardar.
- `currentSourceDocumentPath()` pasa a apuntar a `source/<archivo>` tras guardar.
- `RefreshSourceDocumentUseCase` seguirá usando `currentDocument.sourcePath()`, que ahora es la copia interna.

## Aviso al usuario

Después de guardar un proyecto con documento fuente, la UI muestra un aviso:

> DocuPodcast guardó una copia del documento fuente dentro de la carpeta del proyecto. A partir de ahora, Refrescar contenido leerá esa copia, no el archivo externo original.

El aviso incluye la ruta de la copia interna y un checkbox:

```text
No volver a mostrar este aviso
```

El checkbox se guarda usando `Preferences` del usuario para no repetir el mensaje si el usuario ya lo entendió.

## Regla de UX

El usuario debe entender que si quiere actualizar la lectura después de guardar el proyecto, debe editar la copia dentro de la carpeta `source/` del proyecto y luego usar **Refrescar contenido**.

En una tanda futura puede agregarse una acción explícita:

```text
Reemplazar fuente desde archivo externo...
```

Esa acción sería distinta de **Refrescar contenido**. Refrescar lee la copia interna. Reemplazar importa una nueva fuente externa y la vuelve a copiar dentro del proyecto.

## Corrección adicional del diagnóstico

El diagnóstico local reportó un fallo en `RuntimeBundleManifestUseCaseTest`: el contrato no debía contener la frase `PATH global`. Se ajustó el texto de `BuildRuntimeBundleManifestUseCase` para decir que FFmpeg/FFprobe no dependen de rutas globales del sistema, evitando esa frase exacta y manteniendo la intención del contrato.

## Tests agregados/actualizados

- `ReadableDocumentWorkspaceRepositoryTest` ahora valida que `projectSourceDocument().sourcePath()` apunta a `source/<archivo>` dentro del proyecto.
- `ProjectSourcePortabilityT126ASourceTest` valida:
  - que la materialización devuelve `projectSourceDocument`;
  - que el repositorio rebasa el documento a la copia del proyecto;
  - que el flujo de guardado actualiza la sesión y el ViewModel;
  - que la UI muestra aviso con checkbox “No volver a mostrar este aviso”.

## Criterios de aceptación

- Guardar proyecto copia el documento fuente a `source/`.
- La sesión queda apuntando a la copia interna.
- Refrescar contenido lee la copia interna en la misma sesión, sin requerir cerrar/reabrir.
- El usuario recibe un aviso claro y puede ocultarlo.
- Un proyecto ZIP conserva su fuente documental.
