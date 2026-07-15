# PF2D — Configuración con progreso visible para Voz IA avanzada

## Objetivo

La preparación de motores no debe sentirse como consola ni como script obligatorio para el usuario final. La ruta normal queda dentro de **Configuración → Motores de voz**.

## Contrato de UX

- `Preparar automáticamente` abre una ventana de progreso con `ProgressBar` indeterminado mientras se preparan componentes locales.
- `Descargar modelo oficial` abre una ventana de progreso mientras descarga/copía/verifica archivos grandes.
- `Importar modelo...` usa `DirectoryChooser`, copia el modelo a la carpeta local del programa y muestra progreso.
- Las operaciones no se ejecutan al iniciar la app: solo ocurren por acción explícita del usuario.
- Los scripts quedan como adaptadores técnicos o rescate, no como experiencia principal.

## Validación esperada

- `SettingsDialog` contiene `showOperationProgress`, `OperationProgress` y `ProgressBar`.
- Las acciones largas actualizan una etiqueta de estado visible y habilitan cerrar el diálogo al finalizar o fallar.
- La UI normal conserva lenguaje de producto: Voz IA avanzada y Voz local simple.
