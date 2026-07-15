# Memoria — Tanda 58B: hotfix build verde buffer/video

## Decisión

La primera tanda correspondiente después de la lectura masiva no debe ser un refactor grande. Primero se deja la base verde y se prueba lo mínimo.

## Cambios

- Buffer: frase canónica de espera en `PlaybackBufferPolicy.waitingLabel()`.
- Shell: `DocuPodcastShellViewModel` consume esa política en lugar de escribir una frase propia.
- Video: `ExportSimpleVideoPackageUseCase` declara paquete renderizable/auditable y MP4 final pendiente.
- Tests fuente: se actualizan para proteger contrato real, no strings obsoletos.

## Pendiente inmediato

Después de validar tests en Windows:

1. T58C smoke exploratorio mínimo.
2. T59 criterios de scaffolding/UI.
3. T59A componentes GUI transversales.
4. T59B limpieza UX Documento.
5. T60 refactor coordinadores.
