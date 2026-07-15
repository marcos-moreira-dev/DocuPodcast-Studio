# Hotfix tests Windows v2

Este hotfix corrige cinco fallos detectados al ejecutar `scripts\02-ejecutar-tests.bat` en Windows después del hotfix de scripts/UI.

## Fallos corregidos

1. `LocalTtsProcessConfigurationTest` esperaba `/` en rutas, pero Windows entrega `\`. El test ahora normaliza separadores antes de comparar.
2. `DocxDocumentImporterAdvancedTest` esperaba un texto exacto de advertencia de imagen; el importador ahora emite una advertencia que contiene `Imagen sin descripción`.
3. `AudioWorkspaceSourceTest` seguía esperando `Generar audio mock`; la UI ya habla de producto como `Generar audio`.
4. `ToolbarRecordingPlaybackSourceTest` seguía esperando jerga antigua (`Voz IA/TTS`, `Grabar humano`, `Whisper/STT`, `Play selección`). Ahora valida textos de producto: `Voz IA`, `Grabar voz`, `Audio a texto`, `Reproducir selección`.
5. `VoiceSampleImportUiSourceTest` seguía esperando `Importar muestra` en toolbar; la toolbar usa `Importar voz` para lenguaje de producto, mientras el workspace conserva `Importar muestra para Mi voz`.

## Ajustes adicionales

Se limpió jerga visible en la UI donde seguía apareciendo `Whisper/STT` o `Voz IA/TTS`, sustituyéndola por `Audio a texto` y `Voz IA`. La documentación técnica puede seguir mencionando Whisper/STT como implementación futura, pero los botones y menús deben hablar en términos del usuario.

## Validación realizada en este entorno

No se ejecutó Maven porque el entorno no dispone de `mvn`. Se validó compilación parcial con `javac --release 21` para `domain`, `application` e `infrastructure`.

## Validación esperada en Windows

```bat
scripts\00-verificar-entorno.bat
scripts\03-verificar-toolchain.bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```
