# Tanda 81D — Sidebar izquierdo contextual

## 1. Propósito

T81D implementa el inspector contextual del workspace Documento. La intención es que DocuPodcast Studio deje de distribuir acciones de audio, voz, emoción e imagen en menús o rails secundarios y las concentre donde el usuario trabaja: junto al fragmento seleccionado.

El usuario final no debe pensar en guion interno, jobs, manifests o estructura de importación. Debe hacer clic en una oración y ver opciones claras para esa oración.

## 2. Cambio de modelo visual

Antes de esta tanda, el side dock izquierdo todavía respondía a una lógica documental/técnica:

```text
Estructura
Propiedades
Acciones
Perfil
Diagnóstico
Ayuda
```

Eso era útil para depurar el importador, pero no para la experiencia central de lectura narrada. T81D cambia el criterio a módulos de operación contextual:

```text
Detalles
Audio / Narración
Imagen
```

El inspector queda activo por defecto, con `Detalles` como primera superficie. La hoja central se mantiene enfocada en el documento y la barra flotante de T81C conserva el control global de lectura.

## 3. Módulo Detalles

Archivo: `DocumentContextDetailsPanel`.

Muestra:

- fragmento u oración seleccionada;
- bloque activo;
- rango preciso cuando existe selección por oración;
- recordatorio de que el documento fuente es solo lectura;
- recordatorio de que las capas se guardan en el proyecto;
- nota de que metadatos técnicos viven en diagnóstico, no sobre la hoja.

No muestra:

- `styleName`;
- `styleId`;
- `readingProfile`;
- `classificationSource`;
- metadatos crudos del importador.

## 4. Módulo Audio / Narración

Archivo: `DocumentAudioNarrationPanel`.

Este módulo concentra las decisiones de sonido del fragmento seleccionado. Tiene un selector de origen:

```text
Voz IA
Audio del computador
```

### 4.1. Voz IA

Cuando el origen es `Voz IA`, se muestran acciones relacionadas con síntesis:

```text
Asignar voz IA
Generar audio
Asignar emoción / estilo
Quitar emoción / estilo
Quitar audio asignado
```

La emoción/estilo solo existe aquí porque conceptualmente pertenece a una generación de voz. No se muestra cuando el usuario elige un audio ya grabado.

### 4.2. Audio del computador

Cuando el origen es `Audio del computador`, se muestran acciones directas:

```text
Elegir audio…
Extraer audio de video…
Quitar audio asignado
```

No se separan botones por “efecto”, “ambiente” o “persona hablando”. El usuario es responsable de elegir archivos con nombres claros, por ejemplo `Lucía hablando.wav` o `pájaros cantando.mp3`.

## 5. Módulo Imagen

Archivo: `DocumentImageContextPanel`.

Acciones:

```text
Elegir imagen…
Reemplazar imagen
Quitar imagen
```

El panel derecho de miniaturas, que se terminará de ajustar en T81E, será la navegación visual. Este módulo izquierdo es el lugar operativo para asociar la imagen al fragmento.

## 6. Cambios en rail derecho

`DocumentMediaRailView` deja de agregar `DocumentLayerRailView` como bloque de acciones. A partir de T81D, las acciones repetitivas por fragmento viven en el inspector izquierdo. El rail derecho queda reservado para:

- capas activas;
- mini storyboard;
- imágenes importadas;
- navegación visual al texto asociado.

## 7. Cambios de cerebro mínimos

Aunque T81D es frontend, se agregó soporte mínimo de remoción por tipo de capa para que el inspector pueda ofrecer acciones claras:

- `NarrativeLayerCoordinator.removeFirstAssignmentOfKind(...)`;
- `DocuPodcastShellViewModel.removeImageAssignmentForSelectedDocumentRange()`;
- `DocuPodcastShellViewModel.removeEmotionAssignmentForSelectedDocumentRange()`.

Esto no cambia el contrato del cerebro V1; solo expone operaciones que ya son coherentes con las capas narrativas.

## 8. Componentes GUI transversales

Las superficies nuevas usan componentes existentes:

- `SectionHeader`;
- `InfoBadge`;
- `ActionButtonFactory`;
- `RailActionRow`.

No se agregan botoneras ad hoc en el workspace. Los estilos se concentran en `document-reader.css`.

## 9. Archivos modificados

```text
DocumentWorkspaceView.java
DocumentContextDetailsPanel.java
DocumentAudioNarrationPanel.java
DocumentImageContextPanel.java
DocumentMediaRailView.java
SideDockModuleId.java
SideDockStatePolicy.java
DocuPodcastShellViewModel.java
NarrativeLayerCoordinator.java
document-reader.css
```

## 10. Resultado esperado

Al hacer clic sobre una oración, el usuario tiene un lugar claro para trabajar:

- `Detalles` para saber qué está seleccionado;
- `Audio / Narración` para elegir entre Voz IA o Audio del computador;
- `Imagen` para asociar un visual.

El documento central queda limpio. El rail derecho queda visual. Los menús quedan generales. Configuración sigue como superficie técnica separada.
