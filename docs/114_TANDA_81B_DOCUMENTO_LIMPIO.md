# Tanda 81B — Documento limpio y corrección de Configuración solo desde menú

## Base

T81B se aplica sobre T81A, que inició el rediseño frontal después de la congelación del cerebro V1. El usuario validó que la dirección del `menu bar` era correcta, pero detectó dos problemas de superficie:

1. La pantalla de inicio mostraba una tarjeta de **Configuración** con lenguaje de “bodega técnica”. Eso hacía que Configuración pareciera una zona operativa de la bienvenida, cuando debe abrirse únicamente desde el `menu bar`.
2. El workspace Documento seguía mostrando metadatos técnicos de importación dentro de la hoja, por ejemplo `styleName`, `styleId`, `readingProfile`, `classificationSource` y otros datos útiles para diagnóstico pero distractores para lectura/estudio.

T81B corrige ambos puntos sin tocar el cerebro V1.

## Decisión de producto

La regla queda formalizada así:

- **Configuración** existe como ventana de ajustes persistentes, pero su acceso normal vive exclusivamente en el menú superior `Configuración`.
- La pantalla de inicio debe vender el producto: abrir, escuchar, seleccionar fragmentos, asignar capas opcionales y exportar.
- El documento central debe sentirse como una hoja limpia, no como una tabla de depuración.
- Los metadatos técnicos del importador siguen disponibles en paneles de propiedades/diagnóstico, pero no se imprimen debajo de cada párrafo.

## Cambios de interfaz

### 1. Bienvenida sin tarjeta de Configuración

`WelcomeWorkspaceView` deja de recibir `openSettings` y deja de construir la tarjeta `settingsHint()`.

Antes, la bienvenida mostraba una tarjeta de configuración que hablaba de motores, CPU/GPU, FFmpeg y modelos. Esto era correcto como concepto de arquitectura, pero equivocado como superficie de bienvenida: confundía la pantalla de presentación con una entrada a ajustes técnicos.

Ahora la bienvenida conserva solo acciones de producto:

- `Abrir documento`
- `Abrir proyecto`
- `Nuevo proyecto`

Y el flujo recomendado se mantiene como relato simple:

1. Abre un documento fuente o proyecto existente.
2. Usa lectura en voz alta desde el documento.
3. Haz clic en una oración para trabajar capas.
4. Asigna audio, imagen o emoción solo cuando aporte.
5. Exporta cuando esté listo.

### 2. Configuración solo desde el menú superior

`DocuPodcastShellView` conserva:

```java
Menu configuracion = new Menu("Configuración");
MenuItem abrirConfiguracion = new MenuItem("Configuración…");
```

El workspace de bienvenida y la toolbar no deben mostrar botones o tarjetas de Configuración. Esta decisión evita que el usuario sienta que necesita configurar antes de comprender el producto.

### 3. Texto visible de Configuración menos metafórico

`SettingsDialog` deja de mostrar “Bodega técnica editable” como subtítulo visible y usa:

```text
Ajustes operativos persistentes
```

La metáfora de “zona técnica separada” puede seguir guiando documentación interna, pero la interfaz no debe sonar rudimentaria ni ajena al producto. El usuario abre Configuración desde el menú cuando necesita ajustes; no se le empuja desde la portada.

### 4. Documento sin metadatos técnicos visibles

`DocumentWorkspaceView.blockCard()` ya no agrega:

```java
Label metadata = new Label(metadataSummary(block));
metadata.getStyleClass().add("document-block-metadata");
```

También se elimina `metadataSummary(DocumentBlock block)` de la superficie principal.

Esto remueve de la hoja textos como:

```text
styleName: heading 1 · classificationSource: reading-profile · readingProfile: Documento académico Word · styleId: Heading1
```

El documento central ahora queda orientado a:

- tipo legible del bloque;
- texto del bloque;
- selección de oración;
- resaltado de lectura;
- señales visuales mínimas.

### 5. Los metadatos no desaparecen

Los metadatos siguen en `DocumentPropertiesPanel`:

```java
selected.metadata().forEach((key, value) -> content.getChildren().add(metric(key, value)));
```

Esto mantiene utilidad para diagnóstico sin contaminar la experiencia de lectura. La regla es: **lectura al centro, diagnóstico al panel**.

## Tests y guardarraíles

T81B agrega o ajusta guardarraíles para evitar regresión:

- `DocumentReaderDoesNotExposeMetadataSourceTest`
- `FrontendConfigurationSurfaceSourceTest`
- `WelcomeProductLandingSourceTest`
- `SettingsDialogSourceTest`
- `ExportUiSourceTest`
- `GuideUiSourceTest`
- `AiResourcesUiSourceTest`
- `MarkdownImportUiSourceTest`

Los tests fuente antiguos de T81A que todavía esperaban exportación diagnóstica, narración Markdown visible o recursos IA como acción directa se alinean con el nuevo criterio de navegación:

- Markdown entra como documento fuente en el flujo normal.
- La exportación normal es paquete/audio/video, no reporte diagnóstico.
- Recursos IA viven en Ayuda o avanzado, no como botón global.
- Configuración no aparece en bienvenida ni toolbar.

## Alcance explícitamente no incluido

T81B no implementa todavía:

- barra flotante global de lectura;
- sidebar izquierdo contextual con módulos Detalles, Audio/Narración e Imagen;
- sidebar derecho de miniaturas/medios;
- toolbar compacta con iconos;
- retiro completo de workspaces técnicos de navegación secundaria.

Esos puntos quedan para T81C–T81G.

## Resultado esperado

Al abrir la app:

- la pantalla de inicio presenta el producto, no ajustes;
- Configuración se abre desde el menú superior;
- al cargar un documento, la hoja central ya no muestra metadatos técnicos por bloque;
- el usuario puede concentrarse en leer, escuchar y seleccionar fragmentos.
