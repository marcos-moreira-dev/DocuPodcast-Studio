# XTTS-MODEL-PATH-HF2 + AUDIO-ENGINE-CATALOG-HF1

## XTTS-MODEL-PATH-HF2 corrección

La normalización de rutas de Voz IA avanzada queda centralizada en `XttsModelPathPolicy`.
La política acepta rutas seleccionadas por el usuario como carpeta del modelo, raíz `models`, carpeta `models/tts/xtts` o el archivo `model.pth` seleccionado por error.
El resultado efectivo nunca debe producir la forma heredada `model.pth/model.pth`.

Corrección aplicada sobre HF2:

- una carpeta concreta como `custom-xtts` ya no se convierte artificialmente en `custom-xtts/tts/xtts`;
- una carpeta incompleta que no contiene `model.pth` vuelve a reportar el requisito faltante correcto;
- `XttsTtsCommandTemplate` y descarga oficial conservan preferencia por `models/tts/xtts` local cuando esa carpeta trasplantada ya es usable.

## AUDIO-ENGINE-CATALOG-HF1

Se agrega un catálogo operativo de fuentes de audio para Documento:

- `Voz local simple`, usable solo si Piper/runtime/voz pasan readiness;
- `Voz IA avanzada`, usable solo si el modelo/runtime y la prueba WAV real permiten generar chunks de documento;
- `Modo de prueba`, fallback siempre disponible para validar flujo;
- `Audio del computador`, opción operativa por fragmento.

`DocumentAudioNarrationPanel` ya no construye el combo de Origen únicamente desde el motor activo.
Consulta `documentAudioSourceAvailability()` y filtra por `usableInDocument`.
Si el usuario elige un motor de voz del combo, se persiste ese origen como motor activo para evitar una UI que prometa un motor y genere con otro.

## Criterio de UX

Documento es superficie operativa. No debe mostrar motores rotos ni descargados a medias como opciones disponibles.
Configuración puede mostrar motores no listos con acciones de reparación; Documento solo muestra caminos que el usuario puede usar.
