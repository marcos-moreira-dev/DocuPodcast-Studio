# XTTS FINAL MODELDIR GUARD HF4

Esta correccion blinda el caso persistente `model.pth/model.pth` cuando una configuracion legacy ejecuta `scripts/tts/Voz IA avanzada-file-to-wav.ps1` con `-ModelDir` apuntando a `recursos locales IA avanzada/model.pth`.

## Reglas

- Si `-ModelDir` termina en `model.pth`, PowerShell usa la carpeta padre.
- Si la carpeta legacy normalizada no contiene `model.pth`, `config.json` y `vocab.json`, pero existe `models/tts/xtts`, se usa el modelo portable actual.
- El wrapper Python tambien normaliza rutas con comillas, slash final o doble `model.pth/model.pth`.
- La salida del comando debe mostrar `model-normalizado=` y, si aplica, `model-dir-legacy-o-incompleto`.

## Senal esperada

El error exacto `recursos locales IA avanzada/model.pth/model.pth` ya no debe aparecer. Si el motor falla despues, debe reportar el siguiente problema real: modelo incompleto, Python local, dependencia TTS, PyTorch o CUDA.
