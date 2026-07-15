# T81A — Contrato de navegación y superficies oficiales del frontend

## Propósito de la tanda

T81A inicia el rediseño frontal después de la congelación del cerebro V1. La tanda no busca rehacer toda la interfaz todavía; fija la navegación oficial para que las siguientes tandas de UI no sigan reparando superficies que luego serán retiradas. El objetivo es que DocuPodcast Studio se comporte como un lector narrado con capas contextuales, no como Microsoft Word, no como cabina de producción y no como una lista de módulos técnicos.

La decisión central es separar con claridad tres ideas:

1. **Proyecto DocuPodcast**: carpeta contenedora, archivo `.docupodcast.json`, assets, jobs, audio, storyboard, manifiestos y metadatos derivados.
2. **Documento fuente**: Word/DOCX, PDF con texto nativo, Markdown o TXT abierto por el usuario como material de lectura solo lectura.
3. **Operación contextual**: acciones sobre una oración, fragmento o rango seleccionado, que en las siguientes tandas vivirán en el inspector lateral y no en menús técnicos permanentes.

## Criterio de producto

La interfaz debe usar lenguaje normal de escritorio. Las opciones visibles de la barra de menú deben ser familiares para usuarios de aplicaciones comunes: `Archivo`, `Editar`, `Ver`, `Documento`, `Lectura`, `Herramientas`, `Exportar`, `Configuración` y `Ayuda`.

Se eliminan del menú principal los módulos técnicos como entradas de primer nivel:

- `Narración`
- `Voz`
- `Storyboard`
- `Audio`
- `Reproducción`

Estas capacidades no desaparecen del cerebro. Quedan disponibles como herramientas avanzadas o serán absorbidas por Documento, sidebar contextual, configuración y exportaciones. El usuario normal no debe tener que decidir si entra a “Narración avanzada”, “Jobs de audio” o “Storyboard” para lograr el flujo principal: abrir un documento, escucharlo y asignar capas opcionales.

## Menú Archivo

`Archivo` queda reservado para el proyecto y para operaciones de archivo de alto nivel. En esta tanda se consolida la opción solicitada por el usuario: **Abrir carpeta del proyecto**.

Opciones definidas:

- `Nuevo proyecto…`
- `Abrir proyecto…`
- `Abrir recientes` — queda deshabilitado hasta contar con historial persistente.
- `Abrir carpeta del proyecto`
- `Guardar`
- `Guardar como…`
- `Cerrar proyecto`
- `Salir`

### Criterio

`Archivo` habla del contenedor DocuPodcast. No habla del documento fuente salvo por rutas generales de importación/exportación que se resuelven en otras superficies. La carpeta contenedora es importante porque el proyecto maneja documentos, audios, imágenes, videos usados para extraer audio, jobs y manifests. Abrir carpeta del proyecto evita que el usuario tenga que localizar manualmente recursos derivados.

## Menú Editar

`Editar` queda reducido porque el documento fuente es solo lectura. No se convierte DocuPodcast en procesador de texto.

Opciones definidas:

- `Deshacer` — deshabilitado hasta que exista pila de edición de capas.
- `Rehacer` — deshabilitado hasta que exista pila de edición de capas.
- `Buscar en documento…`
- `Ir a página / fragmento…`
- `Limpiar selección`

### Criterio

No se prioriza `Pegar`, porque no se edita el texto del documento fuente. `Copiar` puede resolverse luego mediante menú contextual o atajos si hace falta, pero no debe dominar la navegación. Lo frecuente no será editar texto; será seleccionar una oración y operar sus capas desde el sidebar contextual.

## Menú Ver

`Ver` agrupa opciones de superficie, zoom, paneles y modo de trabajo.

Opciones definidas:

- `Pantalla completa`
- `Ajustar a ancho`
- `Ajustar a página`
- `Aumentar zoom`
- `Reducir zoom`
- `Restablecer zoom`
- `Mostrar / ocultar miniaturas`
- `Mostrar / ocultar panel lateral`
- `Mostrar / ocultar barra de estado`
- `Modo lectura limpia`
- `Modo avanzado`

### Criterio

`Pantalla completa` se implementa en T81A porque es una opción común y útil para estudiar o leer. Las opciones de zoom y visibilidad quedan como superficie contractual; algunas permanecen deshabilitadas o rutas a Documento hasta que T81B/T81C/T81D implementen el comportamiento visual específico.

## Menú Documento

`Documento` se refiere estrictamente al archivo fuente abierto por el usuario, no al proyecto `.docupodcast`.

Opciones definidas:

- `Abrir / cambiar documento fuente…`
- `Refrescar documento fuente`
- `Preparar lectura`
- `Buscar en documento…`
- `Ir a página / fragmento…`
- `Ver información del documento fuente`
- `Diagnóstico de importación…`

### Criterio

Este menú no gestiona jobs, voces, storyboard ni exportaciones. Su responsabilidad es la fuente documental. El usuario debe poder entender que DOCX, PDF legible, Markdown y TXT son simplemente “documentos”. Por eso se evita `Importar narración Markdown` en la navegación normal: Markdown es documento cuando se abre como fuente; el formato interno de narración `docupodcast-script-v1` no pertenece al flujo principal.

## Menú Lectura

`Lectura` concentra la reproducción del documento.

Opciones definidas:

- `Escuchar documento`
- `Reproducir desde selección`
- `Pausar`
- `Reanudar`
- `Detener`
- `Fragmento anterior`
- `Fragmento siguiente`
- `Velocidad de lectura…`
- `Voz predeterminada…`

### Criterio

El usuario debe pensar en lectura, no en manifest, chunks, jobs, gateways o buffers. Los detalles internos siguen en el cerebro y en Configuración/Diagnóstico. En las siguientes tandas, estas acciones se complementarán con una barra flotante sobre la hoja, concreta y visible, para controlar la lectura global del documento.

## Menú Herramientas

`Herramientas` es el puente hacia operaciones avanzadas que no deben competir con Documento.

Opciones definidas:

- `Validar integridad del proyecto`
- `Ver estado de exportación`
- `Narración interna`
- `Voces y personajes`
- `Jobs de audio`
- `Storyboard`

### Criterio

Se evita `Exportar diagnóstico…` como acción visible normal porque su sentido no es claro para el usuario común. En su lugar, `Ver estado de exportación` comunica una intención operativa: saber qué se puede exportar y qué falta. La validación de integridad también queda en Herramientas porque es útil, pero no forma parte del flujo diario de lectura.

## Menú Exportar

`Exportar` conserva solo salidas comprensibles.

Opciones definidas:

- `Exportar paquete…`
- `Exportar audio final WAV…`
- `Exportar video simple…`

### Criterio

No se exponen exportaciones internas como `Exportar narración Markdown…` ni `Exportar reporte diagnóstico…` en el flujo normal. El paquete auditable sigue conteniendo reportes y manifiestos internos, pero el usuario no necesita verlos como acciones principales.

## Menú Configuración

`Configuración` se mantiene simple porque la ventana emergente de configuración ya tiene secciones internas.

Opciones definidas:

- `Configuración…`
- `Restaurar configuración predeterminada`

### Criterio

La configuración es la bodega técnica del producto. Allí viven motores TTS, STT, modelos, CPU/GPU, FFmpeg, rutas, almacenamiento, audio y rendimiento. El menú no debe duplicar todos esos subapartados porque la ventana de configuración ya cumple esa función.

## Menú Ayuda

`Ayuda` ofrece orientación real de producto.

Opciones definidas:

- `Guía de uso…`
- `Primeros pasos…`
- `Solución de problemas…`
- `Abrir carpeta de diagnóstico`
- `Recursos IA y Markdown…`
- `Acerca de DocuPodcast Studio`
- `Volver al inicio`

### Criterio

La guía no debe ser un volcado técnico. `Primeros pasos` y `Solución de problemas` se mantienen como accesos directos. `Recursos IA y Markdown` queda en ayuda/avanzado, no como flujo principal. La carpeta de diagnóstico se conserva porque puede ser necesaria para soporte, pero no compite con la lectura.

## Pantalla de inicio

T81A actualiza `WelcomeWorkspaceView` como pantalla de presentación del producto. La pantalla inicial funciona como una “vidriera”: debe explicar el producto antes de que el usuario abra un documento.

Cambios clave:

- Usa `ActionButtonFactory` y `ActionBar` para acciones reales y estilizadas.
- La acción principal es `Abrir documento`.
- Se conserva `Abrir proyecto` y `Nuevo proyecto` como acciones secundarias.
- La pantalla habla de abrir, escuchar, ajustar capas y exportar.
- Se elimina la referencia visible al formato interno `docupodcast-script-v1`.
- La configuración se presenta como bodega técnica separada.

## Componentes GUI transversales

T81A respeta la regla de no crear botones nativos hardcodeados dentro de workspaces. La pantalla de inicio usa componentes compartidos:

- `ActionButtonFactory`
- `ActionBar`
- `EmptyStateView`
- `InfoBadge`

La navegación de menú sigue usando `Menu`, `MenuItem` y `SeparatorMenuItem`, que son controles estándar de shell de escritorio y no botones de workspace. Las tandas siguientes deben mantener la misma disciplina: si aparece una nueva acción repetible en el workspace, debe pasar por componente transversal o factory.

## Límites deliberados de T81A

T81A no implementa aún:

- documento limpio sin metadatos (`styleName`, `readingProfile`, etc.);
- inspector izquierdo por fragmento;
- rail derecho de miniaturas refinado;
- toolbar con iconos;
- barra flotante de lectura global;
- eliminación física de vistas avanzadas.

Sí prepara la ruta para hacerlo sin seguir fortaleciendo superficies técnicas que luego se retirarán.

## Guardarraíles agregados

Se agregan tests fuente para congelar esta decisión:

- `MainMenuNavigationSourceTest`
- `WelcomeProductLandingSourceTest`

También se actualiza `VisibleActionContractSourceTest` para que la bienvenida deje de mencionar importación de narración Markdown como paso visible.
