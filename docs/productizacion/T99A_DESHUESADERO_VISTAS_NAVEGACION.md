# T99A — Deshuesadero de vistas y navegación vieja

## Propósito

Esta tanda inicia el rediseño GUI fuerte limpiando navegación histórica antes de construir el MenuBar, Ribbon, Workspace Documento, Sidebar y Rail definitivos.

La regla de producto aplicada es:

```text
No se rediseña encima de vistas heredadas.
Primero se retiran de la navegación normal las superficies que ya no son producto.
```

## Decisión de producto

DocuPodcast Studio tendrá solo tres workspaces reales:

```text
Inicio
Documento
Voces
```

Cada uno tiene un rol claro:

```text
Inicio   → portada funcional y propagandística.
Documento → vista principal: leer, escuchar y configurar oraciones.
Voces    → workspace secundario real para registrar/probar voces de referencia.
```

## Vistas que dejan de ser producto

Estas superficies quedan fuera de la navegación normal:

```text
Guion
Audio Jobs
Diagnóstico
Storyboard
Exportación
Configuración como workspace
```

### Guion

No existe como vista de producto. Si el usuario quiere trabajar sobre un guion, abre `guion.docx` como fuente documental. El guion es una decisión del usuario, no una pantalla que la máquina debe imponer.

### Audio Jobs

No existe como vista. El progreso de generación/renderizado debe mostrarse después como overlay o panel de proceso largo.

### Diagnóstico

No existe como vista. La revisión de integridad o configuración se ejecuta desde acción puntual y devuelve un reporte breve o un diálogo.

### Storyboard

No existe como vista principal. Vive en el rail derecho del Documento, asociado a oraciones/imágenes.

### Configuración

No es workspace. Es ventana secundaria.

### Exportación

No es workspace. Es diálogo o flujo corto de exportación.

## Cambios de navegación

- `WorkspaceSurfacePolicy` considera producto a `WELCOME_HOME`, `DOCUMENT_READER` y `VOICE_LIBRARY`.
- `SCRIPT_EDITOR`, `AUDIO_JOBS` y `STORYBOARD` pasan a superficies internas/heredadas.
- Al restaurar una sesión antigua guardada en `SCRIPT_EDITOR`, `AUDIO_JOBS` o `STORYBOARD`, la app vuelve a `DOCUMENT_READER`.
- `VOICE_LIBRARY` sí sobrevive como workspace real.
- `WorkspaceRouteResolver` normaliza rutas antiguas a una superficie segura.
- `WorkspaceNavigationCoordinator` guarda únicamente superficies de producto en el view state.

## Cambios de shell

- El shell ya no registra vistas heredadas como factories normales.
- Se retiran del menú visible los accesos a:
  - Narración interna.
  - Jobs de audio.
  - Storyboard como workspace.
- Voces se conserva como workspace secundario real.

## Guardarraíles

- No debe reaparecer `Guion` como navegación principal.
- No debe reaparecer `Audio Jobs` como vista operativa.
- No debe reaparecer `Storyboard` como workspace principal.
- No debe reaparecer `Diagnóstico` como vista prioritaria.
- Voces sí debe sobrevivir como workspace secundario real.

## Validación esperada

- La suite normal debe seguir verde.
- Los tests de workspace deben confirmar:
  - Inicio, Documento y Voces son superficies de producto.
  - Guion, Audio Jobs y Storyboard no son navegación de producto.
  - Una ruta persistida antigua vuelve a Documento.
