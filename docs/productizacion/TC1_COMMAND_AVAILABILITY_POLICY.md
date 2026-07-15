# TC1 — CommandAvailabilityPolicy

Estado: implementada sobre RF5 verde.

## Objetivo

Unificar la disponibilidad de comandos visibles para que Menú, Ribbon y adaptadores heredados no tengan reglas duplicadas ni contradictorias sobre cuándo una acción está habilitada.

## Cambios principales

- Se agrega `presentation.command.CommandAvailabilityPolicy` como fuente única de disponibilidad.
- `DocuPodcastShellView.commandItem(...)` ahora enlaza cada `MenuItem` mediante `commandAvailabilityPolicy.disabledBinding(commandId, viewModel)`.
- `RibbonView` elimina su `disabledBinding(...)` local y consume la misma política de comandos.
- `WorkspaceCapabilityPolicy` queda como adaptador legacy: traduce `WorkspaceCapability` a `AppCommandId` usando `WorkspaceCapabilityCommandMapper` y delega en `CommandAvailabilityPolicy`.
- La política también expone `unavailableReason(...)` con mensajes humanos para futuras tooltips/status.

## Reglas cubiertas

- Proyecto requerido: guardar, cerrar, exportar, inspeccionar exportación e importar muestra de voz.
- Proyecto guardable requerido: abrir carpeta, abrir exportaciones e inspeccionar integridad.
- Documento requerido: refrescar fuente, abrir ubicación, preparar lectura y escuchar documento.
- Selección requerida: reproducir selección, asignar voz/audio/imagen y asociar visuales.
- Lectura preparada requerida: generación de audio y secuencia visual interna.
- Job activo requerido: cancelar generación.

## Contrato de producto

No cambia UX, CSS ni componentes visuales. La tanda solo mueve reglas de disponibilidad a una política central. La interfaz gráfica normal conserva nombres amigables y no muestra nombres técnicos de motores.

## Tests

- `CommandAvailabilityPolicyTc1SourceTest` valida la política central, su consumo por Menú/Ribbon/adapter legacy y razones humanas.
- Se actualizan `DocumentPrimaryOperationSourceTest` y `ToolbarContextualWorkspaceSourceTest` para reconocer `CommandAvailabilityPolicy` como dueño de disponibilidad.
