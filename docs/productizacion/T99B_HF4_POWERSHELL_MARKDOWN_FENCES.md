# T99B-HF4 - PowerShell Markdown fences y diagnostico verde

## Objetivo

Corregir el segundo fallo local detectado por `scripts\99-diagnostico-completo.bat` tras T99B-HF3: los preflight de motores ya no fallan por caracteres no ASCII, pero Windows PowerShell 5.1 seguia interpretando las cercas Markdown escritas con comillas dobles como escapes de PowerShell.

## Causa

En PowerShell, el caracter backtick (`) es caracter de escape. Las lineas como:

```text
$content += "```"
```

pueden romper el parser porque la ultima comilla queda escapada por el backtick anterior. El sintoma local fue:

```text
Token 'Coqui/XTTS' inesperado
Token 'Completa' inesperado
Falta la cadena en el terminador: ".
```

## Cambios

- `scripts/tts/preflight-startup-engines.ps1` usa comillas simples para cercas Markdown: `'```bat'` y `'```'`.
- `scripts/tts/preflight-piper-ffmpeg.ps1` usa comillas simples para cercas Markdown: `'```bat'` y `'```'`.
- `scripts/tts/setup-xtts-portable-python.ps1` aplica la misma regla para sus reportes Markdown.
- `ScriptsRootSafeSourceTest` agrega guardarrail para impedir cercas Markdown con comillas dobles en `.ps1`.

## Alcance

No cambia comportamiento productivo ni GUI. Solo corrige compatibilidad de scripts con Windows PowerShell 5.1.

## Resultado esperado

`scripts\99-diagnostico-completo.bat` debe quedar verde en build/tests/smoke del cerebro y los pasos de preflight deben dejar de fallar por parser. Si faltan motores, los reportes deben indicar `REQUIERE_PREPARACION` sin romper la ejecucion del diagnostico base.

## Proxima tanda recomendada

T99C - Deshuesadero visual minimo.
