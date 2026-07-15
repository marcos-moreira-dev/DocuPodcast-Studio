# T98 — Contrato GUI global de DocuPodcast Studio

Este documento consolida las decisiones de interfaz gráfica acordadas antes de entrar al rediseño fuerte. Su función es evitar que el frontend se construya sobre placeholders, duplicaciones o decisiones generadas por inercia. El contrato no es una guía estética suelta: define responsabilidades por superficie, comportamiento esperado y límites de producto.

## 1. Principios rectores

### 1.1 Si aparece, funciona

Toda acción visible debe cumplir una de tres condiciones:

- funciona;
- está deshabilitada con una razón clara;
- está oculta hasta que exista.

No deben quedar botones, menús o pestañas que solo sirvan como promesa. La palabra “avanzado” no debe usarse para esconder funciones incompletas.

### 1.2 Una acción, un comando

Un caso de uso debe tener un dueño visual principal. Si aparece en más de una superficie, todas las superficies deben llamar al mismo `AppCommandId`, no implementar lógica duplicada.

Ejemplo: `Escuchar documento` puede existir como acceso en MenuBar/Ribbon, pero su dueño operativo principal es la playbar flotante del workspace.

### 1.3 Automatizar pasos previos obvios

Si el usuario presiona una acción principal y faltan pasos previos obvios, la app debe encadenarlos con confirmaciones humanas en lugar de bloquear al usuario con precondiciones técnicas.

Ejemplo:

```text
Abrir fuente documental → Escuchar documento
→ si falta proyecto guardado, explicar que se creará un proyecto DocuPodcast
→ Aceptar / Cancelar
→ elegir carpeta
→ crear carpeta contenedora
→ preparar lectura/audio
→ continuar
```

La automatización debe ser clara y reversible: las acciones destructivas o estructurales piden confirmación.

### 1.4 DocuPodcast no edita la fuente documental

La app usa DOCX, PDF, TXT o Markdown como entrada de lectura. Las capas, audios, imágenes, voces, jobs y exportaciones viven en el proyecto `.docupodcast`, no en el archivo fuente.

### 1.5 Whisper/STT fuera del producto visible

Whisper puede quedar encapsulado como infraestructura histórica si ya existe, pero no pertenece al núcleo del producto ni debe aparecer en UI principal, configuración protagonista, guía normal, roadmap visible o criterios de RC.

Motores visibles del producto:

- Coqui/XTTS para calidad;
- Piper como fallback rápido;
- FFmpeg para audio/video/media.

### 1.6 Audio del computador es clip genérico

El programa no clasifica si el audio elegido por el usuario es voz, pájaros, música, ruido, efecto o ambiente. Desde la app es simplemente un `AUDIO_CLIP` que puede asociarse a una oración/rango.

### 1.7 Superficies y responsabilidades

```text
MenuBar      = acciones estructurales/transversales.
Ribbon       = herramientas por modo, no operación fina de fragmento.
Workspace    = uso constante: leer, escuchar, seleccionar oración.
Sidebar izq. = inspector/configurador de la oración seleccionada.
Rail der.    = navegación visual de storyboard/imágenes asignadas.
StatusBar    = estado bajo + control de tamaño de lectura.
Configuración= motores, rutas, rendimiento y diagnóstico operativo.
```

## 2. Mapa final de superficies

```text
App
├─ Inicio
├─ Documento
└─ Voces

Ventanas / overlays
├─ Configuración
├─ Guía de uso
├─ Exportar
├─ Progreso de generación/renderizado
└─ Revisar integridad / diagnóstico puntual
```

Se eliminan como workspaces normales:

- Guion;
- Audio Jobs;
- Diagnóstico;
- Storyboard;
- Exportación.

## 3. Orden recomendado de implementación visual

Antes del rediseño fuerte ya deben estar cerrados los contratos de comandos y componentes transversales. Luego:

```text
GUI-1 — MenuBar final
GUI-2 — Ribbon base
GUI-3 — Workspace Documento limpio
GUI-4 — Sidebar izquierdo + rail derecho
GUI-5 — Playbar flotante
GUI-6 — Configuración / guía / diálogos
GUI-7 — Iconografía / CSS / componentes
GUI-8 — Smoke visual
```

## 4. Guardarraíles de diseño

- No usar emojis como iconografía final.
- No usar botones hardcodeados si existe componente transversal.
- No reintroducir `Audio a texto`, `Whisper` o `STT` en la UI de producto.
- No crear vistas técnicas si pueden ser overlays, diálogos o reportes.
- No duplicar operaciones del sidebar en el Ribbon.
- No mostrar jerga técnica al usuario normal: `manifest`, `job`, `gateway`, `stdout`, `stderr`, `commandTemplate`, etc.
