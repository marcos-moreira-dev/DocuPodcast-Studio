# XTTS-MODEL-PATH-HF1 — normalización de ruta model.pth

## Problema corregido

En pruebas reales de Voz IA avanzada podía aparecer:

```text
FileNotFoundError: .../model.pth/model.pth
```

Esto indica que el runtime recibió como `--model-dir` una ruta que terminaba en `model.pth`. El wrapper Python trataba esa ruta como carpeta y volvía a añadir `model.pth`, generando una ruta inválida.

## Cambio

- `tools/xtts-wrapper/synthesize_xtts.py` normaliza `--model-dir` antes de construir rutas internas.
- `scripts/tts/xtts-file-to-wav.ps1` también normaliza si recibe una ruta terminada en `model.pth`.
- Si el usuario o una configuración vieja apunta al archivo `model.pth`, se usa la carpeta padre como carpeta del modelo.
- El wrapper sigue validando `model.pth`, `config.json` y `vocab.json` antes de cargar Coqui/XTTS.

## Regla vigente

Voz IA avanzada debe recibir una carpeta de modelo XTTS, no el archivo `model.pth`. Si por compatibilidad llega el archivo, DocuPodcast lo corrige para evitar `model.pth/model.pth` y mostrar errores más claros si faltan otros archivos.

## Validación focal

- `XttsModelPathHfSourceTest`
- `VoiceGeneratedTestRealSynthesisPF1SourceTest`
- `py_compile tools/xtts-wrapper/synthesize_xtts.py`
