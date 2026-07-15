# Tanda 56 — Asistente real de modelos

## Propósito

Convertir el asistente de modelos de una superficie descriptiva a una base verificable para instalación/importación local de modelos, sin depender de una URL fija ni exponer línea de comandos al usuario normal.

## Cambios principales

- Se agrega `application.modelsetup` con contratos de carpeta local para XTTS/Coqui, Piper y Whisper local.
- Se agrega `InspectLocalModelFolderUseCase` para revisar carpetas importadas o descargadas por el usuario.
- Se detectan archivos mínimos esperados para cada motor y se reportan faltantes en lenguaje humano.
- Se reconoce la presencia de manifiestos de checksum (`SHA256SUMS.txt`, `checksums.txt`, `model-manifest.json`, `.sha256`).
- El asistente visual en Configuración muestra botones de acción guiada: descargar desde catálogo verificable, importar manualmente, verificar y probar.
- La pantalla Documento no queda contaminada con términos técnicos de motores, checksum ni comandos.

## Contrato de producto

La app debe poder guiar a un usuario no técnico así:

```text
Configurar modelos → elegir motor → descargar/importar → verificar carpeta → probar motor → usar en Documento
```

La descarga real queda pendiente de un manifiesto versionado; la importación manual y la verificación local ya tienen contrato de aplicación.

## Tests

```text
InspectLocalModelFolderUseCaseTest
RealModelAssistantSourceTest
```
