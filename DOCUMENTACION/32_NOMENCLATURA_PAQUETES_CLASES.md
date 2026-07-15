# 32 — Nomenclatura de paquetes y clases

## Paquetes raíz

```text
com.marcosmoreiradev.docupodcaststudio.bootstrap
com.marcosmoreiradev.docupodcaststudio.domain
com.marcosmoreiradev.docupodcaststudio.application
com.marcosmoreiradev.docupodcaststudio.infrastructure
com.marcosmoreiradev.docupodcaststudio.presentation
```

## Evitar nombres heredados

No usar:

```text
DiagramTypeId
DiagramToolbarAction
DiagramWorkbenchView
diagram-*
DmsProject
RenderJob si ya es audio
```

Usar:

```text
WorkspaceKind
WorkspaceToolbarAction
VisualWorkspaceView
storyboard-*
DocuPodcastProject
AudioJob
```

## Prefijos de IDs

```text
DOC-
BLK-
SCR-
SEG-
CHR-
VOC-
STY-
PER-
IMG-
STB-
AUD-
JOB-
NOTE-
```

## Clases de UI

View = nodo JavaFX.
ViewModel = estado observable y comandos UI.
Coordinator = coordina interacción compleja.
UseCase = operación de aplicación.
Gateway/Repository = puerto.
Adapter = implementación infraestructura o adaptador visual.
