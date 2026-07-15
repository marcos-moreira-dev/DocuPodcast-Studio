# RF1 — Continuidad: ReadingComfortCoordinator

Estado: implementada sobre T121-V10 verde.

## Objetivo

Continuar la división de `DocuPodcastShellViewModel` sin cambiar comportamiento visible ni tocar la experiencia de Documento/Voces.

## Implementación

Se agrega:

```text
presentation.shell.workflow.ReadingComfortCoordinator
```

Responsabilidades extraídas:

- carga inicial del tamaño de fuente del lector;
- clamp de tamaño de lectura entre 14 y 28 px;
- cálculo de porcentaje de zoom sobre 18 px = 100%;
- persistencia de `ReadingDocumentSettings.baseFontSize`;
- resultado de persistencia con mensaje humano si guardar settings falla.

`DocuPodcastShellViewModel` conserva las propiedades JavaFX y solo delega el flujo operativo.

## Contrato preservado

- El StatusBar sigue usando `ReadingZoomControl`.
- El lector conserva rango 14–28 px.
- El zoom conserva 18 px como 100%.
- No cambia la interfaz gráfica.
- No se agregan estilos ni componentes visuales nuevos.
- No se reintroducen nombres técnicos de motores en GUI.

## Tests

- `PresentationRefactorRf1SourceTest` valida la extracción.
- `ShellViewModelBrainDebtSourceTest` baja el umbral de deuda a 2500 líneas y exige `ReadingComfortCoordinator` en auditoría.
