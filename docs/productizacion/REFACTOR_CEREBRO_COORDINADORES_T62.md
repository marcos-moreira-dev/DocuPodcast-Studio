# T62 — Refactor prioritario de coordinadores del cerebro

## Propósito

T62 convierte parte de la auditoría del cerebro en coordinadores ejecutables, sin rediseñar todavía la cara visual de DocuPodcast. La prioridad sigue siendo la raíz de producto:

```text
Documento fuente solo lectura → Documento narrable → leer/escuchar → capas opcionales → storyboard/video opcional → exportación
```

## Coordinadores introducidos

### `DocumentIntakeCoordinator`

Responsable de la entrada de documentos fuente. Importa el archivo externo, aplica el perfil de lectura y adjunta el documento narrable al proyecto.

Reglas:

- Word/DOCX, PDF, Markdown/MD y TXT siguen siendo fuentes externas solo lectura.
- El coordinador trabaja sobre el Documento narrable del proyecto, no sobre el archivo fuente.
- La UI no debe reconstruir manualmente la cadena importar → perfilar → adjuntar → estado de proyecto.

### `SourceDocumentRefreshCoordinator`

Responsable de la acción visible **Refrescar contenido**.

Reglas:

- Relee el archivo fuente desde disco.
- Usa `RefreshSourceDocumentUseCase` para comparar snapshots.
- Si el contenido cambió, actualiza el Documento narrable y reporta que audio queda obsoleto y capas/storyboard quedan en revisión.
- No borra silenciosamente audio, capas ni storyboard.
- Nunca edita ni sobrescribe el documento fuente.

### `WorkspaceNavigationCoordinator`

Responsable de recordar el workspace activo sin tratar navegación como edición de contenido.

Reglas:

- Cambiar de Documento a Guion, Audio, Storyboard, Voces o Inicio no debe marcar el proyecto como sucio por sí solo.
- Si el proyecto ya estaba sucio, la navegación no debe limpiar ese estado.
- La persistencia de `activeWorkspace` es estado de vista, no contenido narrativo.

## Ajuste visible mínimo

La pantalla Documento puede exponer **Refrescar contenido** como acción secundaria. No es todavía un rediseño visual: es un punto de entrada a un flujo de cerebro ya modelado.

## Criterio de salida

- El ViewModel empieza a delegar entrada de documento, refresco de fuente y navegación.
- Los tests documentan que el refactor es del cerebro, no de la cara.
- `DocuPodcastShellViewModel` no debe crecer sin límite mientras se extraen coordinadores.
