# T103 — Inicio propagandistico moderno

## Base

T103 parte de T102 verde validada localmente por el usuario con `scripts\\99-diagnostico-completo.bat`.

## Objetivo

Convertir Inicio en una portada funcional y visualmente mas moderna, sin abrir nuevas capacidades ni reintroducir superficies tecnicas.

La referencia visual enviada antes de esta tanda mostro que la pantalla ya tenia Ribbon/StatusBar funcionales, pero el Inicio seguia pareciendo una combinacion de paneles de guia y no una portada clara del producto. T103 compacta la informacion, centra la promesa del producto y usa formas geometricas sobrias para dar direccion visual sin contaminar el flujo principal.

## Cambios productivos

- `WelcomeWorkspaceView` pasa de una composicion tipo columna izquierda/centro/derecha a una portada moderna con hero principal, tarjetas de flujo, panel de alcance V1 y panel de recientes.
- Se mantiene la accion principal `Abrir documento` usando `ActionButtonFactory.primary`.
- Se mantienen acciones reales: `Abrir documento`, `Abrir proyecto` y `Nuevo proyecto`.
- Se incorpora una visual abstracta de documento/voz/paquete mediante formas JavaFX, no imagenes externas.
- Se mantiene la honestidad de formato: DOCX, PDF con texto nativo, Markdown y TXT.
- Se conserva la advertencia de PDF: solo texto nativo extraible; PDF escaneado queda fuera de V1.
- Recientes queda como empty state explicito, no como lista falsa.

## Reglas mantenidas

- Inicio no muestra Configuracion como tarjeta operativa.
- Inicio no expone Whisper/STT.
- Inicio no menciona Jobs, manifests ni workspaces internos.
- Inicio no crea botones JavaFX hardcodeados; usa componentes transversales.
- No se implementan recientes reales todavia.
- No se modifica el cerebro.

## Archivos principales

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java`
- `src/main/resources/css/welcome.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeModernHomeT103SourceTest.java`

## Validacion en entorno ChatGPT

- Verificacion de strings de contrato de Inicio.
- Ejecucion reflexiva de tests fuente focales.
- Verificacion de que Inicio no reintroduce Configuracion, Whisper/STT, Jobs ni botones hardcodeados.
- ZIP integro.

Maven completo no se ejecuto porque el entorno ChatGPT no tiene `mvn`.

## Siguiente tanda

T104 — Documento limpio tipo lector Word.
