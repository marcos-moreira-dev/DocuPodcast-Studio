# 04 — Alcance y no alcance

## Alcance inicial

- JavaFX desktop autocontenido.
- Java 21 Eclipse Temurin con Maven Toolchain.
- DOCX como entrada primaria.
- Markdown como intercambio humano/IA.
- TXT y PDF simple como entradas secundarias.
- Guion narrable editable.
- Audio por segmentos con progreso/ETA.
- Jobs reanudables.
- Biblioteca de voces básica.
- Storyboard vivo básico: una imagen por segmento.
- Proyecto `.docupodcast.json` con assets relativos.
- Exportación de guion Markdown, audio WAV y paquete de proyecto.

## No alcance inicial

- Editor Word completo.
- Interpretación automática de imágenes por IA.
- Generación automática de película.
- Animación compleja.
- Alineación palabra por palabra perfecta.
- Clonación de voces de terceros sin autorización.
- Exportación de video compleja desde el MVP.
- Generación emocional garantizada si el motor no la soporta.

## Frontera honesta

La app puede permitir un estilo “enojado” o “triste” como intención de interpretación, pero debe saber si el motor puede cumplirlo. Si no, lo muestra como no disponible o lo degrada con advertencia.

## Regla de producto

No se debe mostrar una feature visible si no existe cadena mínima:

```text
UI → caso de uso → dominio/infraestructura → test o smoke
```
