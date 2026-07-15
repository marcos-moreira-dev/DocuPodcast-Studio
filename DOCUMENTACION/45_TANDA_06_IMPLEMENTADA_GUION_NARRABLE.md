# Tanda 6 implementada — Guion narrable

## Propósito

Esta tanda convierte el documento Word/DOCX ya importado, revisado y clasificado mediante Reading Profile en un **guion narrable** editable. El guion se vuelve el artefacto central para audio, voces, estilos, storyboard y playback futuro.

## Decisiones aplicadas

- El guion narrable es un **documento estructurado**, no un canvas.
- Cada segmento usa ID estable `SEG-###`.
- Cada segmento referencia su bloque de origen (`B001`, `B002`, etc.).
- Cada segmento incluye placeholders explícitos para personaje, voz y estilo:
  - `CHR-NARRATOR`
  - `VOC-NARRATOR`
  - `STY-NEUTRAL`
- El audio todavía no se genera en esta tanda; queda para Tanda 7.
- El script se materializa en `script/narration-script.json` y se registra como asset relativo `SCRIPT-001`.

## Código agregado

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/script/
src/main/java/com/marcosmoreiradev/docupodcaststudio/application/script/
src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/script/
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/
```

## Flujo implementado

```text
Word/DOCX
→ ReadableDocument
→ ReadingProfile aplicado
→ Crear guion
→ NarrationScriptDocument
→ ScriptWorkspaceView
→ Guardar proyecto
→ script/narration-script.json
→ asset NARRATION_SCRIPT
```

## UI

Se agregó workspace Guion con:

- botón “Crear desde documento”;
- listado de segmentos;
- tarjetas de segmento;
- resumen de palabras/segmentos/caracteres;
- validación básica;
- acceso desde menú y toolbar.

## Persistencia

Al guardar un proyecto que tiene guion, se crea:

```text
script/narration-script.json
```

y se registra en `.docupodcast.json` el asset:

```text
SCRIPT-001 → script/narration-script.json
```

## Validación parcial

En este entorno se validó con:

```text
javac --release 21
```

para:

```text
domain
application
infrastructure
```

Maven completo queda pendiente en entorno local con Temurin 21 + Maven Toolchain.
