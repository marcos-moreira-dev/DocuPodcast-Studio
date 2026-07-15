# Tanda 88B — Hotfix media tests y criterio de audio genérico

## Motivo

La validación Maven en Windows detectó fallos en T88:

- `UserMediaFormatPolicyTest.documentsSupportedFormatSummaryForSimpleUiCopy` esperaba el texto `MP3/WAV`.
- `FfmpegAudioNormalizationGatewayTest` y `FfmpegVideoAudioExtractionGatewayTest` usaban fake executables `.sh`, inválidos en Windows.
- `WhisperCppSpeechToTextGatewayTest` no generaba correctamente el transcript falso en Windows por el parsing del parámetro `-of`.
- `FfmpegMediaT88SourceTest` esperaba una referencia explícita a `AudioNormalizationProfile.SPEECH_TO_TEXT` dentro del gateway FFmpeg.

## Cambios

- `UserMediaFormatPolicy.supportedFormatSummary()` vuelve a incluir `MP3/WAV` sin perder `M4A/FLAC/OGG`.
- Los tests fake de FFmpeg crean `.cmd` en Windows y `.sh` en Unix.
- Los gateways FFmpeg envuelven `.cmd/.bat` con `cmd /c` en Windows.
- El test fake de Whisper captura `-of` sin bloque parenthesized problemático.
- El texto del panel `Audio / Narración` aclara que `Audio del computador` es un clip genérico: puede ser voz, efecto, ambiente o cualquier sonido elegido por el usuario.

## Criterio de producto

El programa no debe inferir ni clasificar semánticamente el contenido del clip. Desde producto, `Audio del computador` relaciona un archivo de audio con una selección del documento. Si el archivo contiene una persona hablando, pájaros, música, ruido o cualquier otro sonido, esa decisión pertenece al usuario.

## Validación en entorno ChatGPT

- Compilación focal con `javac --release 21` de dominio, application e infraestructura media/STT: OK.
- Compilación sintáctica de tests modificados con stubs JUnit mínimos: OK.
- Checks fuente de T88: OK.
- Maven completo no se ejecutó porque `mvn` no está instalado en este entorno.
