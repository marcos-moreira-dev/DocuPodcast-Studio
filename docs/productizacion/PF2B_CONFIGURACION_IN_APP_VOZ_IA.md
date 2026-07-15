# PF2B — Configuración in-app de Voz IA avanzada

PF2B responde a la preocupación de producto: el usuario final no debe ejecutar un `.bat` como experiencia normal ni repetirlo en cada arranque. La preparación de Voz IA avanzada pasa a estar integrada en **Configuración → Motores de voz**.

## Cambios principales

- Se agrega `PrepareXttsPortableRuntimeUseCase` para preparar el runtime local desde la app, bajo demanda.
- Se agrega `XttsRuntimePreparationReport` para devolver estado, reporte, salida técnica y readiness posterior.
- Se agrega `ImportXttsModelFolderUseCase` para importar una carpeta local de modelo desde la app hacia `models/tts/xtts`.
- Se agrega `XttsModelImportReport` para informar archivos copiados, carpeta destino y estado humano.
- `SettingsApplicationServices` expone preparación/importación de Voz IA avanzada.
- `ApplicationServicesFactory` cablea los nuevos casos de uso.
- `SettingsDialog` ahora muestra botones reales:
  - **Verificar Voz IA avanzada**
  - **Preparar automáticamente**
  - **Importar modelo...**
  - **Usar Voz IA avanzada**
- La selección de Voz IA avanzada ya no se hace a ciegas: primero verifica readiness y bloquea si faltan runtime/modelo/voz.
- El script queda como adaptador técnico interno; la UX normal vive dentro de la app.
- `ModelFolderContract.xttsHighQuality()` exige `model.pth`, `config.json` y `vocab.json`, alineado con el wrapper/checker local.
- `EngineSetupCard` deja de presentar acciones como “no botón”.

## Decisión de producto

La app no prepara motores al iniciar. Solo lo hace cuando el usuario pulsa **Preparar automáticamente** y acepta la confirmación. Esto evita arranques lentos, descargas sorpresivas y modificaciones ocultas.

La preparación automática descarga/prepara Python y dependencias dentro de `tools/`, sin modificar PATH global. El modelo se verifica después; si falta o se obtuvo por una fuente externa, se importa desde el botón **Importar modelo...**.

## Validaciones añadidas

- `AdvancedVoiceInAppSetupPf2BSourceTest`
- `ImportXttsModelFolderUseCaseTest`

Además se revalidaron los guardarraíles PF1/PF1B/PF2A relacionados con voz real, hotfix de storyboard y runtime portable.
