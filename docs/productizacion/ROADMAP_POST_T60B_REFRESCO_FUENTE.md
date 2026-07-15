# Roadmap post T60B — fuente inmutable con refresco controlado

## Estado

T60 fijó el Documento narrable como raíz V1. T60B corrige los source tests de documentación arrastrados por el cambio de versión y agrega el contrato de **Refrescar contenido** para documentos fuente solo lectura.

## Próximas tandas

| Tanda | Objetivo | Salida |
|---|---|---|
| T61 | Auditoría ejecutable del cerebro con refresco de fuente | Mapa de clases/casos de uso y deudas verificables. |
| T62 | Refactor prioritario de coordinadores | Reducir ViewModel y aislar documento, refresco, playback, audio y capas. |
| T63 | Round-trip funcional | Guardar/reabrir documento, snapshot, audio, capas, storyboard y reportes. |
| T64 | Configuración operativa | Motores, modelos, buffer, FFmpeg, STT y diagnóstico persistente. |
| T65 | Rediseño UI aplicado | Documento limpio con botón Refrescar contenido y menos cabina técnica. |
| T66 | Smoke integral / RC | Evidencia real, instalable, límites conocidos. |

## Regla de implementación

No implementar un botón `Refrescar contenido` como simple handler de vista. Primero debe existir contrato de cerebro: snapshot, detector, reporte, política de obsolescencia y coordinador.
