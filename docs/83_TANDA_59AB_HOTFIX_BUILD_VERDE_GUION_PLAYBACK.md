# Tanda 59A-B — Hotfix build verde: Guion / playback

## Propósito

Tanda 59A-B corrige una regresión introducida por la migración de botones hardcodeados hacia componentes GUI transversales en Tanda 59A.

La funcionalidad de reproducción desde selección en el workspace Guion seguía existiendo, pero el source test `ScriptWorkspaceRecordingPlaybackSourceTest` buscaba únicamente la forma antigua con llamada directa `viewModel.playFromSelectedSegment`. Tras migrar a `ActionButtonFactory`, el código usa la referencia de método `viewModel::playFromSelectedSegment`, por lo que el guardarraíl quedó demasiado acoplado a la sintaxis.

## Cambio aplicado

Se actualiza el test para aceptar ambas formas válidas:

```text
viewModel.playFromSelectedSegment
viewModel::playFromSelectedSegment
```

No se cambia comportamiento productivo. La acción visible `Reproducir desde selección` sigue existiendo y continúa conectada al ViewModel.

## Decisión de producto

Esta tanda también deja explícito que el trabajo de componentes GUI transversales debe separarse de dos frentes posteriores:

1. Catálogo/contrato de componentes transversales: lenguaje, roles, variantes y criterios de uso.
2. Auditoría/refactor del cerebro de la app: dominio, casos de uso, coordinadores, servicios, persistencia, audio, playback, video, jobs, configuración y fronteras.

La UI final todavía no está cerrada; el cerebro sí debe quedar ordenado antes de seguir puliendo pantallas.

## Criterio de salida

```bat
scripts\02-ejecutar-tests.bat
```

Debe terminar con build verde y cero fallos.
