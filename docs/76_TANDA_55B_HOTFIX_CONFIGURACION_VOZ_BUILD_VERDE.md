# Tanda 55B — Hotfix configuración de voz y build verde

## Propósito

Esta tanda corrige la superficie textual de la pantalla **Configuración → TTS y voces** para alinearla con el guardarraíl de calibración de voz agregado en la Tanda 55.

## Corrección aplicada

La fila `Muestras de voz` ahora expone explícitamente la guía:

> Haz clic para grabar tu voz o una voz autorizada: neutral, enojada, triste, feliz e intrigada.

La frase permanece dentro de Configuración, no dentro del workspace Documento, para mantener la pantalla principal limpia y orientada a leer/escuchar documentos.

## Alcance

No cambia motores, audio, TTS real, selección de documento, exportación, video, persistencia ni UI principal. Es un hotfix de build y contrato de configuración.

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

El test fuente `VoiceCalibrationSettingsSourceTest` debe pasar junto con el resto de la suite.
