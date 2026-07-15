# T87 — whisper.cpp STT real

## Resumen ejecutivo

T87 deja el camino STT operativo para V1: audio → texto real con `whisper.cpp`. `whisper.cpp` es el motor oficial de audio a texto. Se mantiene el criterio de pocos motores:

- Piper: fallback/liviano de TTS.
- Coqui/XTTS: voz de calidad alta.
- whisper.cpp: STT local.
- FFmpeg: media/audio/video auxiliar.

## Rutas locales esperadas

```text
tools/whisper-cpp/whisper-cli.exe
models/stt/whisper/<modelo>.bin
```

## Contrato técnico

El contrato real de STT queda así:

```text
SpeechToTextRequest(projectFile, sourceAudioFile, segmentId, language)
→ JavaSoundAudioNormalizer
→ stt/input/<token>-stt-input.wav
→ whisper.cpp -m <model> -f <wav> -l <language> -otxt -of <outputBase>
→ stt/transcripts/<token>.txt
→ stt/logs/<token>-stdout.log / stderr.log
→ SpeechToTextResult
```

Todos los artefactos generados deben quedar dentro de la carpeta contenedora del proyecto.

## Script de soporte

`script/stt/whisper-file-to-text.ps1` existe como ayuda operativa y documentación ejecutable del contrato CLI. El gateway puede usar directamente `whisper-cli.exe`, pero el script permite pruebas manuales y futura integración guiada.

## Reglas de UX

- Documento no debe mostrar flags como `-m`, `-otxt` o rutas técnicas.
- Configuración puede mostrar ejecutable/modelo/timeout.
- Los errores deben convertirse en mensajes humanos en T89.
- STT no se habilita como flujo final si falta ejecutable o modelo.

## Supuesto de uso de motores

El usuario indicó que no planea usar Coqui/modelos/voces como mercancía. El repositorio documenta este supuesto como uso local/demostrativo/no venta directa. Si se empaquetan o distribuyen motores/modelos de terceros, se debe revisar licencia antes de RC real.

## Guardarraíles

- `WhisperCppSpeechToTextGatewayTest`
- `WhisperCppRealSttSourceTest`
- `WhisperCppConfigurationTest`
- `TranscribeAudioToTextUseCaseTest`

## Pendientes

- UI humana de configuración/probar STT.
- Mensajes no técnicos para el usuario normal.
- Smoke real con `whisper-cli.exe` y modelo descargado localmente.
