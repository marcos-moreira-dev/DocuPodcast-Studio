# XTTS-MODEL-PATH-HF2 — Política única de ruta de modelo IA avanzada

## Objetivo

Eliminar definitivamente el caso `model.pth/model.pth` antes de que el comando llegue a PowerShell o Python.

La app puede recibir rutas heredadas o seleccionadas por el usuario en varias formas:

- carpeta raíz `models`;
- carpeta concreta `models/tts/xtts`;
- carpeta trasplantada que contiene directamente `config.json`, `model.pth` y `vocab.json`;
- archivo `model.pth` seleccionado o persistido por error.

Todas esas formas se normalizan ahora desde Java mediante `XttsModelPathPolicy`.

## Cambios principales

- Se agregó `application/modelsetup/XttsModelPathPolicy` como autoridad Java para ruta de modelo XTTS.
- `XttsTtsCommandTemplate` usa la política antes de construir `-ModelDir`.
- `InspectXttsSetupReadinessUseCase` usa la misma política para inspección.
- `ImportXttsModelFolderUseCase` normaliza selección de carpeta/archivo antes de inspeccionar/copiar.
- `DownloadXttsOfficialModelUseCase` descarga en la ruta normalizada por la política.
- Se agregaron tests unitarios y source tests para bloquear regresiones.

## Regla de producto

La app nunca debe pasar a PowerShell/Python una ruta que apunte a `model.pth` como si fuera carpeta del modelo.

Si la configuración o selección apunta a:

```text
.../recursos locales IA avanzada/model.pth
```

la app debe usar:

```text
.../recursos locales IA avanzada
```

## Guardarraíles

- `XttsModelPathPolicyTest`
- `XttsTtsCommandTemplateTest.managedCommandNormalizesSettingsThatPointToModelPth`
- `XttsModelPathHf2SourceTest`

## Validación en entorno ChatGPT

- `javac` focal de clases nuevas/modificadas.
- `javac` focal de tests nuevos/modificados con stubs JUnit.
- Ejecución reflexiva de source tests.
- Ejecución manual mínima de la política de ruta.

Maven completo no se ejecutó porque `mvn` no está instalado en el entorno.
