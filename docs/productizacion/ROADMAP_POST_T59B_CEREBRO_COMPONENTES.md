# Roadmap post T59B — cerebro primero, cara después

## Contexto

T59 fijó principios rectores. T59A inició una primera ejecución de componentes GUI. T59A-B dejó esa base verde. T59B corrige el foco: antes de seguir puliendo la cara visual, hay que separar formalmente el catálogo de componentes y el mapa del cerebro de la app.

## Decisión estratégica

```text
No se debe confundir catálogo transversal con aplicación visual masiva.
No se debe confundir rediseño de UI con refactor del cerebro.
```

El producto todavía puede cambiar de cara, pero el cerebro debe ordenarse ya.

## Orden recomendado

| Tanda | Foco | Tipo |
|---|---|---|
| T59B | Catálogo transversal + mapa del cerebro. | Documental/arquitectura protegida. |
| T59C | Contrato de documentos fuente solo lectura + referencia ofimática futura. | Documental/arquitectura protegida. |
| T60 | Auditoría ejecutable del cerebro. | Source audit + tests + mapa de responsabilidades. |
| T61 | Refactor prioritario de coordinadores. | Código sin cambio visual profundo. |
| T62 | Round-trip funcional real. | Persistencia, jobs, capas, assets, reapertura. |
| T63 | Configuración operativa. | Motores, modelos, buffer, FFmpeg, STT, preferencias. |
| T64 | Rediseño UI aplicado. | Documento limpio + componentes con criterios ya definidos. |
| T65 | Smoke integral y RC. | Pruebas manuales, packaging, límites conocidos. |

## T60 — Auditoría ejecutable del cerebro

Debe medir y documentar:

- responsabilidades de `DocuPodcastShellViewModel`;
- flujos que modifican proyecto;
- acciones que dependen de capacidad;
- casos de uso por subsistema;
- persistencia y round-trip;
- jobs/audio/playback/video;
- configuración y diagnóstico;
- tests faltantes.

Salida esperada:

```text
AUDITORIA_CEREBRO_RESULTADO_T60.md
BrainResponsibilityMapSourceTest
ShellViewModelResponsibilityBudgetSourceTest
RoundTripGapMatrixSourceTest
```

## T61 — Refactor prioritario

Debe extraer coordinadores sin rediseño visual profundo:

- `DocumentNarrationCoordinator`;
- `DocumentSelectionCoordinator`;
- `NarrativeLayerCoordinator`;
- `PlaybackWorkflowCoordinator`;
- `AudioWorkflowCoordinator`;
- `WorkspaceNavigationCoordinator`.

Salida esperada: menos lógica concentrada en el ViewModel y tests equivalentes verdes.

## T62 — Round-trip funcional

Debe demostrar que se puede abrir, escuchar, asignar, guardar, cerrar y reabrir sin pérdida.

## T63 — Configuración operativa

Debe convertir la bodega técnica en preferencias persistentes y pruebas guiadas.

## T64 — Rediseño aplicado

Solo después del cerebro ordenado se debe limpiar la cara visible: Documento principal, rails, SideDock, Guion avanzado, Audio/Jobs y Guía.

## Regla final

```text
Primero gobernanza, luego cerebro, luego cara.
```


## Ajuste T59C

Antes de T60 queda fijado que Word/DOCX, PDF, Markdown/MD y TXT fuente se abren en modo solo lectura. T60 debe auditar el cerebro bajo esta frontera.
