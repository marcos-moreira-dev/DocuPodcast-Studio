# Refactor del cerebro — AudioWorkflowCoordinator T64

## Regla

El audio es parte del cerebro operativo de la aplicación, no de la cara principal. El usuario debe poder leer/escuchar desde Documento sin entender jobs, snapshots, manifest, gateway ni process-diagnostics.

## Responsabilidades extraídas

`AudioWorkflowCoordinator` centraliza:

```text
engineDescriptor
request
submit
resume
cancel
persistedJobs
recoverableSnapshot
selectedSnapshot
jobDetailLines
diagnosticLabels
```

## Responsabilidades que permanecen temporalmente en el ShellViewModel

El shell conserva por ahora:

- propiedades JavaFX visibles;
- `activeAudioJobStatus`;
- `audioJobRunning`;
- registro final de assets de audio;
- integración con playback y documento;
- estado de barra/status.

Estas responsabilidades deben seguir bajando en tandas posteriores, pero T64 evita un refactor riesgoso de una sola vez.

## Criterio de salida

La app debe seguir verde y el ViewModel debe empezar a depender de coordinadores para la lógica de audio persistido y reanudación.
