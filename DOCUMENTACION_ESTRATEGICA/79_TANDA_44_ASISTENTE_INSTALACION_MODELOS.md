# Tanda 44 — Asistente de instalación de modelos

## Objetivo

Preparar una superficie guiada para instalar, importar, verificar y probar modelos locales de voz y STT sin convertir la pantalla principal en una cabina técnica ni exigir línea de comandos al usuario normal.

## Alcance implementado

- Se agrega `ModelInstallAssistantCatalog` con planes para:
  - `XTTS / Coqui` como motor potente prioritario.
  - `Piper` como motor liviano/semipotente de respaldo.
  - `Whisper local` para STT/audio a texto.
- Se agregan `ModelInstallPlan` y `ModelInstallStep` para describir carpeta, estrategia, importación manual, verificación y pasos.
- Se agrega `ModelInstallAssistantView` como componente visual reutilizable dentro de Configuración.
- `SettingsDialog` incorpora la sección `Asistente / modelos`.
- Se agregan estilos modulares `ui-model-*` dentro de `css/components/settings-shell.css`.
- Se agregan tests fuente para asegurar que no haya URL fija ni comandos crudos en la experiencia normal.

## Decisiones de producto

El asistente no debe depender de un enlace único externo. Las descargas reales, cuando se conecten, deberán venir de un manifiesto versionado con checksums y con alternativa de importación manual. Esto evita que el producto falle si cambia Hugging Face, GitHub, una release o cualquier proveedor externo.

La pantalla Documento sigue limpia: no muestra XTTS, Piper, Whisper, checksums ni carpetas de modelos. Todo eso vive en Configuración.

## Fuera de alcance

- Descarga real de modelos.
- Verificación real por checksum.
- Ejecución real de preflight desde botón.
- Prueba de voz/STT real desde Configuración.
- Instalador completo con modelos empaquetados.

## Validación recomendada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Tests nuevos:

```text
ModelInstallAssistantCatalogTest
ModelInstallAssistantSourceTest
```
