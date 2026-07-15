# Smoke automático del cerebro — T79

T79 agrega un smoke ejecutable de núcleo. El escenario vive en `BrainSmokeScenarioTest` y debe correr sin JavaFX.

## Contrato

El smoke debe cubrir, en un solo flujo reproducible:

- importación documental V1: DOCX, TXT, Markdown y PDF nativo;
- rechazo de PDF sin texto nativo;
- narración interna desde Documento;
- capa real de imagen y storyboard derivado;
- audio mock persistido;
- playback manifest;
- round-trip completo;
- integridad del proyecto;
- preparación de exportaciones;
- bundle auditable;
- paquete de video simple.

## Ejecución focalizada

```bat
scripts\18-smoke-automatico-cerebro.bat
```

La suite completa sigue siendo:

```bat
scripts\02-ejecutar-tests.bat
```

## Evidencia

El escenario escribe reportes bajo:

```text
target/docupodcast-smoke/
```

El archivo principal es `SMOKE_REPORT.md`.

## Gobierno

Este smoke evita regresar a validaciones puramente manuales para el cerebro. Los checklists visuales siguen existiendo, pero no deben reemplazar la prueba automática del núcleo.
