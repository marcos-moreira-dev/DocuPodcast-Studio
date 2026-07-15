# T60 — Auditoría ejecutable del cerebro

## Propósito

T60 convierte el mapa del cerebro en criterios auditables. No rediseña la cara visual ni hace refactor profundo todavía. Su tarea es fijar qué se debe medir antes de mover responsabilidades.

## Criterio raíz agregado

```text
El Documento narrable es el objeto padre de la experiencia V1.
El guion es una proyección interna/avanzada de narración, no una raíz paralela para el usuario normal.
```

## Auditorías ejecutables propuestas

| Auditoría | Qué debe detectar | Resultado esperado |
|---|---|---|
| Raíz Documento narrable | Flujos que presenten guion como paso obligatorio para usuario normal. | Marcar como deuda de lenguaje/producto. |
| Fuente inmutable | Flujos que parezcan editar Word/PDF/MD/TXT original. | Deben moverse a artefactos del proyecto o exportaciones. |
| ViewModel gigante | Responsabilidades concentradas en `DocuPodcastShellViewModel`. | Extraer coordinadores por flujo. |
| Navegación dirty | `withViewState(... activeWorkspace ...)` que marca contenido como modificado. | Separar estado visual de dirty de contenido. |
| Capas con placeholders | `targetId` pendientes que se guardan como si fueran reales. | Validar target real o mostrar placeholder explícito. |
| Storyboard de una imagen para varios textos | Rail/indexación por `imageAssetId` que pierda usos múltiples. | Mostrar todos los rangos/segmentos asociados. |
| Video honesto | Paquete renderizable comunicado como MP4 final. | Diferenciar paquete, render en progreso y MP4 real. |
| Settings operativas | Configuración que solo informa y no persiste/aplica. | Guardar preferencias y permitir pruebas guiadas. |

## Mapa de flujos por cerebro

### Abrir documento

```text
archivo fuente solo lectura
→ perfil de fuente
→ importador/extractor
→ ReadableDocument / Documento narrable
→ diagnóstico separado
```

No debe crear en pantalla una ruta obligatoria hacia Guion. Si internamente genera segmentos, deben servir al Documento narrable.

### Escuchar documento

```text
Documento narrable
→ segmentación/proyección de narración interna
→ audio por segmentos
→ manifest/playback
→ resaltado en documento
```

El usuario normal no debería tener que entrar al workspace Guion para escuchar.

### Asociar imagen/storyboard

```text
selección de texto en Documento narrable
→ asset de imagen real o placeholder explícito
→ binding a rango/segmento
→ duración igual a audio del texto asociado al renderizar
```

### Guardar y reabrir

```text
proyecto DocuPodcast
→ documento narrable materializado
→ capas
→ audio/jobs
→ storyboard/bindings
→ settings cuando existan
```

El archivo fuente no se sobrescribe.

## Deuda observada que T61 debe atacar

| Deuda | Acción prioritaria |
|---|---|
| `DocuPodcastShellViewModel` supera las dos mil líneas. | Extraer coordinadores de documento, selección, playback, audio, capas, storyboard/video y settings. |
| Guion aparece como concepto demasiado visible. | Recontratarlo como vista avanzada/proyección de narración. |
| Dirty state por navegación. | Separar navegación visual de cambios persistibles. |
| Storyboard/video dependen de segmentos. | Asegurar trazabilidad hacia rangos del Documento narrable. |
| Configuración técnica aún no es plenamente operativa. | Persistir settings y pruebas guiadas. |

## Regla para no romper producto

T60 no debe borrar clases ni renombrar masivamente `NarrationScript*`. Primero fija el contrato. El refactor T61 debe ser incremental y con tests verdes después de cada extracción.
