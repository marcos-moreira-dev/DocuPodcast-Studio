# Estrategia — Tanda 59 — Rediseño guiado por principios

## Por qué esta tanda existe

El proyecto ya tiene demasiada funcionalidad para seguir improvisando UI. Antes de limpiar workspaces o extraer coordinadores, se requiere un contrato rector que diga qué se considera producto, qué se considera scaffolding y qué debe quedar en Avanzado.

## Línea estratégica

La app no se simplifica eliminando capacidades; se simplifica decidiendo qué se ve por defecto.

```text
Usuario normal: Documento + escuchar + lectura cómoda.
Usuario intermedio: capas, voces, imágenes, storyboard simple.
Usuario avanzado: guion, audio jobs, motores, STT, FFmpeg, diagnóstico.
```

## Criterio de éxito

Una persona debe poder probar DocuPodcast sin entender qué es un job, un manifest, un gateway, un prebuffer o un render package.

## Riesgos controlados

- Refactor prematuro sin saber qué flujo funciona.
- UI saturada por acumulación de paneles.
- Componentes duplicados por vista.
- Promesas de capacidades que todavía son contratos o paquetes.
- CSS modular con tokens huérfanos.
