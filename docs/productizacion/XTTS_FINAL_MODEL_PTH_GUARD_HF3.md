# XTTS Final ModelDir Guard HF3

## Objetivo

Cerrar el caso persistente donde una configuracion legacy de Voz IA avanzada llega a ejecutar `scripts/tts/Voz IA avanzada-file-to-wav.ps1` con `-ModelDir .../model.pth` y Coqui/XTTS termina intentando abrir `model.pth/model.pth`.

## Decision

La app repara ese caso en todos los bordes relevantes:

1. `OperationalSettingsMigrationPolicy` convierte comandos legacy/localizados a motor administrado `xtts`.
2. `InspectXttsSetupReadinessUseCase` repara settings antes de inspeccionar modelo/runtime.
3. `RunXttsReadinessSmokeUseCase` repara settings antes de generar el WAV de prueba.
4. `SettingsAwareVoiceTestSynthesisGateway` persiste best-effort los settings reparados antes de sintetizar.
5. `XttsTtsCommandTemplate` repara settings antes de resolver comando, seleccion, modelo y muestra.
6. `scripts/tts/xtts-file-to-wav.ps1` normaliza cualquier `ModelDir` que termine en `model.pth` a carpeta padre.
7. `scripts/tts/Voz IA avanzada-file-to-wav.ps1` hace la misma normalizacion y luego redirige al script administrado.
8. `tools/xtts-wrapper/synthesize_xtts.py` rechaza el uso de una carpeta literal `model.pth` como raiz de modelo y usa siempre la carpeta padre cuando `--model-dir` termina en `model.pth`.

## Diagnostico esperado

Si vuelve a aparecer exactamente:

```text
FileNotFoundError ... model.pth/model.pth
```

la causa mas probable es que el usuario siga ejecutando una carpeta vieja que no contiene este parche. En la version corregida, los logs deben incluir al menos una de estas marcas:

```text
DOCUPODCAST_XTTS_PS: model-dir-termina-en-model-pth; usando carpeta padre
DOCUPODCAST_XTTS_PS: model-normalizado=...
DOCUPODCAST_XTTS_LEGACY_PS: model-dir-termina-en-model-pth; usando carpeta padre
DOCUPODCAST_XTTS_LEGACY_PS: model-normalizado=...
DOCUPODCAST_XTTS: model_dir_termina_en_model_pth; usando_carpeta_padre
```

Si el modelo sigue incompleto, el error correcto despues de este parche debe hablar de archivos faltantes en una carpeta real, no de `model.pth/model.pth`.

## Regla de producto

La Voz IA avanzada no debe avanzar a diagnostico GPU/CUDA hasta que la prueba WAV en CPU pueda cargar el modelo correctamente. La GPU solo entra despues de que modelo, Python local, wrapper y muestra de referencia funcionen.
