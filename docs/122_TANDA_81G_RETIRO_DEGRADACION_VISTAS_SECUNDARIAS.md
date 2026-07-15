# Tanda 81G — Retiro/degradación de vistas secundarias redundantes

## Propósito

Esta tanda cierra la etapa de rediseño frontal previa al Release Candidate con una decisión de navegación: **DocuPodcast Studio no debe comportarse como una suite de módulos técnicos**, sino como un lector narrado donde el documento fuente ocupa el centro operativo.

Hasta T81F ya existían mejoras importantes: menú común, bienvenida estilizada, documento limpio, barra flotante de lectura, inspector izquierdo contextual, rail derecho de miniaturas y toolbar con iconos. Sin embargo, seguían existiendo workspaces productivos como `Guion`, `Audio`, `Voces` y `Storyboard`. Esas vistas tienen valor técnico y pueden seguir existiendo para usuarios avanzados o mantenimiento, pero no deben competir como navegación principal con el flujo normal:

```text
Abrir documento → leer/escuchar → seleccionar fragmento → ajustar audio/imagen si hace falta → exportar
```

## Corrección incluida de T81F

El log local de T81F reportó un fallo en `DocumentSmartPlaybackActionSourceTest`. El test todavía buscaba la forma antigua de bindear el texto del botón principal de lectura dentro de `MainToolbarView`:

```text
button.textProperty().bind(viewModel.documentPrimaryActionLabelProperty())
```

Esa forma ya no aplica desde T81F porque la toolbar global utiliza el componente transversal `ToolbarActionButton`, que encapsula icono, texto, tooltip y estilo. La corrección no revierte el componente. Actualiza el contrato de test para validar que:

- `MainToolbarView` sigue usando `viewModel.documentPrimaryActionLabelProperty()`.
- La toolbar usa `ToolbarActionButton` como componente transversal.
- La acción inteligente de lectura sigue vinculada al estado de selección/documento.

## Decisión de producto

### Superficies principales

Solo dos superficies forman parte de la navegación primaria:

1. **Inicio**
2. **Documento**

`Inicio` es la pantalla de entrada/propaganda operativa. `Documento` es la raíz funcional: hoja limpia, barra flotante de lectura, inspector izquierdo contextual y rail derecho de medios.

### Superficies avanzadas

Las siguientes vistas permanecen implementadas, pero se degradan a herramientas avanzadas:

- `SCRIPT_EDITOR` / Guion narrable
- `AUDIO_JOBS` / Jobs de audio
- `VOICE_LIBRARY` / Biblioteca de voces
- `STORYBOARD` / Storyboard completo

Esto significa que no se eliminan clases ni capacidades. Se evita que sean la forma normal de operar la aplicación.

## Cambio técnico principal

Se agrega `WorkspaceSurfacePolicy`, que formaliza la diferencia entre superficies principales y avanzadas:

```text
PRIMARY: WELCOME_HOME, DOCUMENT_READER
ADVANCED: SCRIPT_EDITOR, AUDIO_JOBS, VOICE_LIBRARY, STORYBOARD
```

La política permite:

- preguntar si un workspace pertenece a la navegación principal;
- preguntar si un workspace es avanzado;
- restaurar un workspace persistido de forma segura.

## Restauración de proyectos

Cuando un proyecto viejo fue guardado con una vista avanzada activa, por ejemplo `AUDIO_JOBS`, la app ya no debe abrir directamente en esa cabina técnica. T81G hace que la restauración vuelva a `DOCUMENT_READER`.

Ejemplo:

```text
viewState.activeWorkspace = AUDIO_JOBS
→ al reabrir: DOCUMENT_READER
```

Esto conserva el proyecto y sus artefactos, pero devuelve al usuario al espacio donde realmente lee y opera el documento.

## Menú Herramientas

El menú `Herramientas` conserva acceso a superficies avanzadas, pero sus etiquetas ahora son explícitas:

```text
Narración interna (avanzado)
Voces y personajes (avanzado)
Jobs de audio (avanzado)
Storyboard (avanzado)
```

La intención es evitar confusión: estas opciones existen para inspección, soporte, depuración o operación avanzada, no para el flujo diario.

## Qué NO cambia

- No se elimina el cerebro de guion, voces, audio, storyboard ni jobs.
- No se elimina la capacidad de abrir esas vistas desde herramientas avanzadas.
- No se rompe la generación de audio ni el playback.
- No se mueve Configuración fuera del menú bar.
- No se agregan botones técnicos a la toolbar.

## Guardarraíles

Se agregan/actualizan pruebas para proteger que:

- `SCRIPT_EDITOR`, `AUDIO_JOBS`, `VOICE_LIBRARY` y `STORYBOARD` estén implementados pero no en navegación primaria.
- Un workspace avanzado persistido restaure a `DOCUMENT_READER`.
- El menú marque explícitamente estas vistas como avanzadas.
- La acción de lectura inteligente siga viva con `ToolbarActionButton`.

## Resultado esperado

Después de T81G, la aplicación debe entenderse así:

```text
Inicio = entrada y presentación del producto
Documento = operación diaria
Configuración = ventana técnica desde menú bar
Herramientas avanzadas = inspección y soporte
```

El usuario normal no tiene que entender guiones internos, jobs, manifests, colas, motores ni storyboard completo para cargar y escuchar un documento.
