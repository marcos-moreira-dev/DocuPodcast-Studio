# Tanda 66 — Round-trip funcional real

Tanda basada en T65 verde validada por el usuario.

## Objetivo

Agregar una pieza ejecutable de round-trip funcional para verificar que el proyecto DocuPodcast pueda guardarse, cerrarse y reabrirse con sus artefactos principales.

## Cambios productivos

- `ProjectRoundTripRequest`
- `ProjectRoundTripResult`
- `ProjectRoundTripUseCase`
- `ProjectApplicationServices` expone `projectRoundTrip`
- `ApplicationServicesFactory` cablea el caso de uso con repositorios reales

## Alcance

El round-trip cubre:

- Documento narrable materializado
- Proyección interna de narración
- Capas narrativas guardadas en `.docupodcast.json`
- Imagen/asset asociado
- Storyboard materializado
- Job de audio persistido bajo `jobs/`
- Reapertura mediante repositorios existentes

## No cambia

- No rediseña UI.
- No convierte DocuPodcast en editor de Word/PDF/Markdown/TXT.
- No sobrescribe documentos fuente.

## Validación local esperada

```bat
scripts\02-ejecutar-tests.bat
```
