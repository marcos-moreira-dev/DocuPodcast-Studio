# PF1 — Voz generada real en Vista Voces

Estado: implementada sobre TV1.

## Objetivo

Cerrar el placeholder funcional más crítico de la Vista Voces: la acción **Generar prueba con esta voz** ya no debe crear un WAV sintético propio del caso de uso. La prueba generada debe pasar por el motor local configurado, igual que la ruta productiva de audio del documento.

## Cambios principales

- `GenerateVoiceTestUseCase` deja de escribir audio artificial y depende de un puerto `VoiceTestSynthesisGateway`.
- Se agregan `VoiceTestSynthesisRequest` y `VoiceTestSynthesisResult` como contrato de prueba corta de voz.
- Se agrega `LocalProcessVoiceTestSynthesisGateway`, que ejecuta el comando TTS local configurado mediante `ProcessBuilder`, genera el TXT auditable de entrada y valida que el WAV exista y tenga contenido real.
- `InfrastructureServices` expone el gateway de prueba de voz y `ApplicationServicesFactory` lo inyecta en `GenerateVoiceTestUseCase`.
- `LocalTtsProcessConfiguration` agrega `commandForVoiceTest(...)` y soporta `{speakerWav}`, `{referenceSample}` y `{voiceSample}` para permitir que la prueba de Voz IA avanzada use la muestra seleccionada.
- Si el comando convencional ya trae `-SpeakerWav` o `--speaker-wav`, la prueba de voz puede reemplazar ese argumento por la muestra resuelta desde la biblioteca.
- El modo **Modo de prueba** ahora bloquea generación de prueba de voz real con mensaje humano; queda como diagnóstico de flujo, no como sustituto de voz.
- La prueba local simple usa el mismo puerto de síntesis, sin muestras humanas ni tonos avanzados.
- `tools/xtts-wrapper/synthesize_xtts.py` deja de cargar `model_path=model_dir` y deja de caer a descarga automática; ahora exige `model.pth`, `config.json` y `vocab.json` dentro del modelo local.

## Archivos clave

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/GenerateVoiceTestUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceTestSynthesisGateway.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceTestSynthesisRequest.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceTestSynthesisResult.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalProcessVoiceTestSynthesisGateway.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessConfiguration.java`
- `tools/xtts-wrapper/synthesize_xtts.py`

## Tests y guardarraíles

- `GenerateVoiceTestUseCaseTest` valida que el caso de uso genera manifiesto con `realSynthesis=true`, usa el puerto de síntesis y falla fuerte si la muestra registrada no existe.
- `VoiceGeneratedTestRealSynthesisPF1SourceTest` impide que vuelva la ruta de generación WAV sintética dentro del caso de uso productivo y protege la carga local de XTTS sin fallback de descarga.

## Validación realizada en este entorno

No se ejecutó Maven completo porque este entorno no tiene `mvn` instalado. Se validó con:

- `javac` focal de los nuevos contratos de aplicación, el caso de uso, `LocalTtsProcessConfiguration`, `LocalProcessVoiceTestSynthesisGateway` y factories de bootstrap.
- Compilación focal de tests con stubs JUnit mínimos.
- Ejecución reflexiva focal de 10 tests relacionados con PF1, T121-V05 y T121-V06.

## Siguiente tanda recomendada

PF2 — runtime autocontenido de Voz IA avanzada: preparar Python local, dependencias, modelo XTTS, preflight corto real y scripts de un clic para que Configuración pueda dejar el motor listo sin manipulación manual.
