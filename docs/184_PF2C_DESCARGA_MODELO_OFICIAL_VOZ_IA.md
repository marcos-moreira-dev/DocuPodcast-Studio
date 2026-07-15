# PF2C — Descarga in-app del modelo oficial de Voz IA avanzada

PF2C completa la inquietud de producto: desde Configuración el usuario puede pedir que la app descargue el modelo oficial de Voz IA avanzada en la carpeta local del programa, sin ejecutar comandos manuales.

## Cambios principales

- Agrega `DownloadXttsOfficialModelUseCase`.
- Agrega `XttsModelDownloadReport`.
- `module-info.java` agrega `requires java.net.http`.
- `SettingsApplicationServices` expone `downloadXttsOfficialModel`.
- `ApplicationServicesFactory` cablea el downloader.
- `SettingsDialog` añade el botón **Descargar modelo oficial** dentro de **Configuración → Motores de voz**.
- La descarga se ejecuta en hilo de fondo y solo después de una confirmación explícita del usuario.
- El usuario es informado de que la descarga es grande, requiere Internet y queda en `models/tts/xtts`.

## Archivos descargados/verificados

- `config.json`
- `model.pth`
- `vocab.json`
- `speakers_xtts.pth`
- `dvae.pth`
- `mel_stats.pth`
- `LICENSE.txt`
- `README.md`
- `hash.md5`

## Contrato reforzado

`ModelFolderContract.xttsHighQuality()` y `check_xtts_runtime.py` ahora exigen también los archivos auxiliares oficiales: `speakers_xtts.pth`, `dvae.pth` y `mel_stats.pth`. Esto evita declarar listo un modelo incompleto.

## Límite consciente

La app no descarga nada al iniciar y no instala nada en PATH global. La descarga del modelo solo ocurre si el usuario confirma la acción en Configuración. El flujo manual de importación sigue existiendo para entornos sin Internet, modelos ya descargados o instalaciones privadas.
