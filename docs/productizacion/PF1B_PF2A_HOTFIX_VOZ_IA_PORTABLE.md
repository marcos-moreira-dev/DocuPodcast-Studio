# PF1B + PF2A — Hotfix compile y preparación guiada de Voz IA avanzada

Estado: implementada sobre PF1.

## Motivo

El diagnóstico `20260604-013606.zip` falló antes de ejecutar pruebas funcionales de voz. Maven no compilaba por una referencia heredada en `ScriptWorkspaceView`: la vista seguía importando `VisualesDocument` y llamando `currentVisualesProperty()`, mientras el modelo vigente ya usa `StoryboardDocument` y `currentStoryboardProperty()`.

## Corrección PF1B

- `ScriptWorkspaceView` queda alineado con `StoryboardDocument`.
- Se elimina la dependencia rota a `VisualesDocument`.
- Se agrega `ScriptWorkspaceStoryboardHotfixPf1BSourceTest` para impedir que vuelva esa API eliminada.

## Avance PF2A

Se inicia la siguiente tanda de productización de voz local/autocontenida sin esperar otra lectura masiva:

- `tools/xtts-wrapper/check_xtts_runtime.py` ahora valida archivos concretos del modelo local: `config.json`, `model.pth` y `vocab.json`.
- `scripts/tts/setup-xtts-portable-python.ps1` acepta `-LocalTtsRepo`, para instalar el paquete TTS desde un repositorio local entregado por el usuario en vez de depender únicamente de PyPI.
- Se agrega `scripts/30-preparar-voz-ia-avanzada-local.bat` como asistente de una sola entrada para preparar runtime Python local, verificar modelo/muestra y ejecutar smoke WAV corto.
- `scripts/04-verificar-tts-config.bat` deja de afirmar que se usará mock solo porque no existe `DOCUPODCAST_TTS_COMMAND`; ahora lee `~/.docupodcast-studio/operational-settings.properties` y distingue Modo de prueba, Voz IA avanzada y Voz local simple.

## Comandos recomendados en Windows

```bat
scripts\99-diagnostico-completo.bat
```

Para preparar la Voz IA avanzada con el repositorio TTS local:

```bat
scripts\30-preparar-voz-ia-avanzada-local.bat C:\ruta\a\TTS-dev
```

Si no pasas una ruta, el asistente usa el requirements empaquetado:

```bat
scripts\30-preparar-voz-ia-avanzada-local.bat
```

## Límites honestos

Esta tanda no incluye los pesos del modelo XTTS dentro del ZIP. El runtime puede prepararse, pero para generar voz real se necesita una carpeta local `models\tts\xtts` con `config.json`, `model.pth`, `vocab.json` y una muestra WAV válida.
