# Roadmap post T80B — Contenedor antes de media flexible

T80B se inserta antes de la entrada flexible de media porque corrige una regla de persistencia/UX: el proyecto debe vivir dentro de una carpeta contenedora.

## Siguiente tanda recomendada

### T80C — Entrada flexible de media: MP3/WAV/video→audio

Objetivo: permitir una acción simple **Asignar audio** que acepte MP3/WAV y opcionalmente video para extraer solo audio cuando FFmpeg esté disponible.

La carpeta contenedora de T80B será la base para persistir:

```text
assets/original/
assets/audio/
assets/derived/
```

y registrar manifests, checksums y archivos derivados sin ensuciar la ubicación elegida por el usuario.

## Luego

- T80 — Congelación del cerebro V1.
- Lectura 15 — frontend quirúrgico.
- T81 — rediseño frontal guiado.
- T82 — Release Candidate.
