# Léeme primero — por qué existe esta carpeta

Esta carpeta existe porque el proyecto **DocuPodcast Studio** nació de una conversación larga y rica. El objetivo es que el repositorio conserve el contexto aunque el chat original se pierda, se vuelva inmanejable o se abra una nueva sesión.

La carpeta `DOCUMENTACION_ESTRATEGICA/` está deliberadamente llena de archivos `.md`. No es relleno accidental: es documentación viva, handoff extendido y mapa de implementación. Debe leerse como memoria de producto, arquitectura y decisiones.

## Idea central vigente del producto

DocuPodcast Studio es una aplicación JavaFX autocontenida para abrir documentos fuente, preparar su lectura y escucharlos con audio y visuales asociados. El producto vigente no presenta guion ni storyboard como rutas principales de usuario.

Flujo vigente:

1. documento fuente navegable;
2. lectura preparada interna;
3. voces y muestras autorizadas;
4. audio generado o audio del computador asociado al texto;
5. visuales/secuencia visual con imágenes aportadas por el usuario;
6. reproducción sincronizada texto + audio + visual;
7. exportaciones de podcast, paquete de proyecto, reportes y paquete de video simple auditable.

Frase corta vigente:

> Documento fuente → lectura preparada → voces/audio/visuales.

## Referencias técnicas usadas

- **Domain Model Studio/UENS**: referencia principal para shell, tabs, toolbar contextual, SideDock, workspaces, tema claro, guía integrada, persistencia JSON, Markdown, recursos IA, exportación y tests.
- **Fractal Render Studio**: referencia principal para jobs largos, progreso, cola, cancelación cooperativa, métricas y procesamiento por lotes.
- **Proyecto IA/TTS previo del usuario**: referencia de motor de voz local, motor avanzado de voz, motor local simple y experiencia de voz natural. Whisper/STT ya no forma parte del producto vigente.

## Entrada prioritaria

La entrada principal del MVP es **Word/DOCX**, porque el usuario tiene sus notas ahí. Markdown es importante, pero como puente humano/IA y formato importable avanzado, no como flujo inicial obligatorio.

## Entorno fijado

- Java 21.
- Eclipse Temurin.
- Maven Toolchain.
- JavaFX 21.

## Cómo leer esta carpeta

Empieza por estos archivos:

1. `01_RESUMEN_PRODUCTO.md`.
2. `02_DECISIONES_CERRADAS.md`.
3. `05_ENTORNO_JAVA_21_TEMURIN_TOOLCHAIN.md`.
4. `24_ROADMAP_MVP.md`.
5. `PLAN_IMPLEMENTACION_DETALLADO/00_INDICE_TANDAS.md`.
