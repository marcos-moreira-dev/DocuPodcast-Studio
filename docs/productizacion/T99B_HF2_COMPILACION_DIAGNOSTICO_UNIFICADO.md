# T99B-HF2 — Fix de compilación y diagnóstico unificado

## Objetivo

Esta tanda corrige un fallo de compilación detectado en Windows durante `scripts\02-ejecutar-tests.bat` y agrega un script único de diagnóstico para recopilar evidencia completa sin depender de ejecutar muchos scripts manuales uno por uno.

## Corrección productiva mínima

Se corrigió `DocuPodcastShellView` para llamar al método vigente del ViewModel:

```java
viewModel.importUserAudioForSelectedDocumentRange(file.toPath());
```

El código anterior llamaba a `importAudioForSelectedDocumentRange(Path)`, método que ya no existe en `DocuPodcastShellViewModel`.

## Diagnóstico unificado

Se agrega:

```bat
scripts\99-diagnostico-completo.bat
```

El script ejecuta una pasada amplia y no se detiene en el primer fallo. Genera resumen y logs en:

```text
target\diagnostico-completo\<fecha>\
```

También intenta generar un ZIP de la evidencia:

```text
target\diagnostico-completo\<fecha>.zip
```

## Pasos cubiertos

- Java version.
- Maven version.
- `scripts\00-verificar-entorno.bat`.
- `scripts\03-verificar-toolchain.bat`.
- `scripts\04-verificar-tts-config.bat`.
- `mvn -DskipTests compile`.
- `mvn test`.
- `scripts\18-smoke-automatico-cerebro.bat`.
- `scripts\23-preflight-arranque-motores.bat`.
- `scripts\24-verificar-piper-ffmpeg-local.bat`.

Los smokes de motores reales son opt-in:

```bat
scripts\99-diagnostico-completo.bat --real-engines
```

o:

```bat
set DOCUPODCAST_RUN_REAL_ENGINES=1
scripts\99-diagnostico-completo.bat
```

## Guardarraíl agregado

`FfmpegMediaT88SourceTest` ahora verifica que `DocuPodcastShellView` use el método vigente `importUserAudioForSelectedDocumentRange` y no el nombre legacy `importAudioForSelectedDocumentRange`.

## Alcance deliberadamente excluido

Esta tanda no implementa T99C, no rediseña GUI, no toca Ribbon, no modifica comportamiento de audio/media y no cambia contratos de producto. Es una corrección de base para poder continuar con una evidencia de build más clara.
