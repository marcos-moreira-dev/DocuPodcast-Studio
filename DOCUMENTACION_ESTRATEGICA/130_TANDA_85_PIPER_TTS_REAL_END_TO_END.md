# Tanda 85 — Piper TTS real end-to-end

## Objetivo

Esta tanda cierra el primer camino técnico para que DocuPodcast Studio pueda pasar de una lectura mock a una generación real de audio con un motor local liviano. La decisión de producto es deliberada:

- **Coqui XTTS sigue siendo obligatorio** para la calidad alta final.
- **Piper** se usa como primera ruta funcional, promedio/liviana, para demostrar que el flujo completo Documento → texto segmentado → WAV real → reproducción vale la pena.
- El usuario normal no debe escribir comandos en el workspace Documento.
- La configuración puede seguir siendo técnica, pero Documento debe mostrar solo estados humanos.

## Qué se agregó

Se agregó `PiperTtsCommandTemplate`, una fábrica de comando que permite convertir `engineMode=piper` en un comando real si el usuario no escribió una plantilla manual.

Rutas convencionales adoptadas:

```text
scripts/tts/piper-file-to-wav.ps1
tools/piper/piper.exe
models/tts/piper/voices/<voz>.onnx
```

La aplicación ya tenía `LocalTtsProcessAudioGenerationGateway`; esta tanda no duplica ese gateway. En lugar de eso, lo alimenta con una plantilla Piper concreta cuando corresponde.

## Por qué hace falta un wrapper

Piper suele leer texto desde `stdin` y escribir un WAV con `--output_file`. DocuPodcast, en cambio, guarda cada segmento como archivo `.txt` para permitir reanudación, diagnóstico y auditoría de jobs. El wrapper PowerShell hace de puente:

1. Recibe `-Text {textFile}`.
2. Lee el texto persistido del segmento.
3. Lo envía por pipeline a `piper.exe`.
4. Pide a Piper escribir `-Output {outputFile}`.
5. Verifica que el WAV exista y tenga tamaño mayor al header mínimo.

Esto conserva la arquitectura de jobs persistentes sin exigir que Piper soporte directamente `--input-file`.

## Flujo esperado

Cuando `operational-settings.properties` tenga:

```properties
tts.engineMode=piper
tts.voiceProfileId=es_ES-test-medium
```

Y existan:

```text
tools/piper/piper.exe
scripts/tts/piper-file-to-wav.ps1
models/tts/piper/voices/es_ES-test-medium.onnx
```

la fábrica resuelve internamente una plantilla equivalente a:

```text
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts/tts/piper-file-to-wav.ps1" -Piper "tools/piper/piper.exe" -Model "models/tts/piper/voices/es_ES-test-medium.onnx" -Text {textFile} -Output {outputFile}
```

Luego `LocalTtsProcessAudioGenerationGateway` usa esa plantilla por segmento.

## Qué no se hizo

- No se descargó Piper automáticamente.
- No se incluyó ninguna voz `.onnx` pesada dentro del repositorio.
- No se reemplazó Coqui XTTS.
- No se cambió la UI principal para exponer comandos.
- No se convirtió Documento en panel técnico.

## Corrección incluida

Se corrigió el source test `DocumentSmartPlaybackActionSourceTest`, que seguía validando el contrato antiguo del botón inteligente de lectura. La acción de lectura puede estar en toolbar y/o barra flotante, siempre que use componentes transversales y el `documentPrimaryActionLabel` siga saliendo del ViewModel.

## Validación esperada local

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Y luego, cuando se tenga Piper real:

1. Colocar `piper.exe` en `tools/piper/piper.exe`.
2. Colocar una voz `.onnx` en `models/tts/piper/voices/`.
3. Seleccionar `engineMode=piper` y la voz en configuración.
4. Abrir un documento.
5. Pulsar `Escuchar documento`.
6. Confirmar que se generan WAVs en el job y se reproduce audio real.

## Próxima tanda

La siguiente tanda debe ser **T86 — Coqui XTTS wrapper real**, porque Piper es útil para demostrar flujo, pero la calidad alta objetivo requiere Coqui.
