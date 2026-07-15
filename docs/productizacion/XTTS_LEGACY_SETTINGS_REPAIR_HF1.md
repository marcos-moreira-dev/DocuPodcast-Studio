# XTTS-LEGACY-SETTINGS-REPAIR-HF1

## Propósito

Reparar configuraciones persistentes antiguas de Voz IA avanzada que todavía apuntan a scripts localizados o carpetas viejas, por ejemplo `scripts/tts/Voz IA avanzada-file-to-wav.ps1`, `componentes locales IA avanzada-wrapper` o `recursos locales IA avanzada/model.pth`.

## Problema real observado

La prueba de Voz IA avanzada podía seguir ejecutando un comando guardado en `%USERPROFILE%/.docupodcast-studio/operational-settings.properties` aunque el ZIP nuevo ya tuviera el script corregido. Eso producía nuevamente `model.pth/model.pth`.

## Contrato nuevo

Al cargar o guardar settings, `OperationalSettingsMigrationPolicy` repara:

- comando legacy/localizado de XTTS -> `engineMode=xtts`, `commandTemplate=` vacío;
- `storage.modelsDirectory` que termina en `model.pth` -> carpeta padre.

Luego `XttsTtsCommandTemplate` reconstruye el comando administrado desde la carpeta activa de la app.

## Resultado esperado

La app ya no ejecuta rutas absolutas viejas de otra carpeta/tanda para Voz IA avanzada. Si XTTS falla, el error siguiente debe ser modelo incompleto, Python local, PyTorch/CUDA o muestra faltante; no `model.pth/model.pth`.
