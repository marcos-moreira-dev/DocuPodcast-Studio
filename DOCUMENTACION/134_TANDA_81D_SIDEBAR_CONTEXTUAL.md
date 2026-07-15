# Tanda 81D — Sidebar izquierdo contextual

La Tanda 81D implementa el inspector contextual del workspace Documento. La pantalla queda organizada para que el usuario seleccione una oración y opere desde un panel izquierdo con módulos simples: `Detalles`, `Audio / Narración` e `Imagen`.

## Resumen funcional

- `DocumentWorkspaceView` monta `WorkspaceSideDock` con módulos contextuales.
- El side dock ya no abre como estructura/perfil/diagnóstico; ahora es inspector operativo del fragmento.
- El módulo `Audio / Narración` permite elegir entre `Voz IA` y `Audio del computador`.
- Para `Audio del computador`, las acciones visibles son `Elegir audio…` y `Extraer audio de video…`.
- Emoción/estilo solo aparece con `Voz IA`.
- El rail derecho deja de contener acciones de asignación y queda reservado a miniaturas/capas/storyboard.

## Archivos principales

```text
DocumentContextDetailsPanel.java
DocumentAudioNarrationPanel.java
DocumentImageContextPanel.java
DocumentWorkspaceView.java
DocumentMediaRailView.java
SideDockModuleId.java
SideDockStatePolicy.java
NarrativeLayerCoordinator.java
DocuPodcastShellViewModel.java
document-reader.css
```

## Criterio de producto

Documento es la raíz operativa. Las acciones repetitivas sobre una oración deben vivir cerca de la selección, no en menús principales ni vistas técnicas. La configuración sigue separada y el documento fuente sigue siendo solo lectura.
