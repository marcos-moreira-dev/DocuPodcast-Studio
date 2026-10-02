# Corrección post-freeze: resultados por página

Fecha: 2026-08-10  
Estado del lector: **FROZEN = YES**. Esta corrección solo modifica orquestación, resultado y presentación.

## Evidencia física del proyecto

Proyecto inspeccionado: `D:\Proyectos\Demostracion_limite_notable`.

| Página | Último estado observado | Canónica | Regiones | Cobertura/verificación |
|---|---|---:|---:|---|
| 1 | `COMPLETED` | sí | 9 | `ACCEPTED`; verificador sí; recovery no; revisión 1; firma congelada |
| 2 | `FAILED` más reciente; dos intentos anteriores `TRUNCATED` | no | 0 | los intentos truncados agotaron 1000 tokens sin `DONE`; el último intento terminó por caída del runtime visual |
| 3 | `INSUFFICIENT_EVIDENCE` | no | 0 | huecos significativos tras verificador y una recuperación de 3 ROI |

El hover de P1 y su ausencia en P2/P3 son coherentes con la publicación atómica: solo P1 posee `PreparedPdfPage`. No se añadieron overlays artificiales.

## Causa del comportamiento anterior

`PreparePdfScopeUseCase` convertía todo resultado no exitoso en `failedPages` y emitía el copy ambiguo «Página N terminada». `PdfNarratablePreparationCoordinator` convertía después un scope sin páginas preparadas en una `IllegalStateException("Páginas fallidas: ...")`. Esa excepción era artificial y destruía la diferencia entre rechazo semántico y fallo técnico.

Ahora `PreparePdfPageResult` reutiliza `PdfOperationAttemptState` y conserva estado, categoría, motivo humano, causa original y disponibilidad de página canónica previa. El scope agrega outcomes en `TOTAL_SUCCESS`, `PARTIAL_SUCCESS` o `TOTAL_FAILURE` sin rollback de páginas aceptadas.

## Política de comandos

| Comando | ACCEPTED | REJECTED | FAILED |
|---|---|---|---|
| Escuchar documento | reutiliza/genera audio faltante y reproduce | omite audio provisional; no repite el rechazo dentro de la sesión; continúa buscando página aceptada | informa fallo; un nuevo clic es reintento manual |
| Escuchar desde selección | reproduce desde página canónica | no crea audio ni overlay | conserva causa y no crea audio |
| Procesar selección/desde aquí | reutiliza cache vigente o procesa | permite reintento manual explícito | permite reintento manual explícito |
| Procesar lectura completa | conserva aceptadas y continúa tras incidencia local | contabiliza no interpretable y continúa | contabiliza fallo local; un fallo global del batch sigue deteniendo el scope |
| Detalles de preparación/diálogo | muestra estado preparado | muestra `INSUFFICIENT_EVIDENCE` y si existe canónica previa | muestra categoría y root cause solo en detalles técnicos |

Una página aceptada vigente sigue siendo semantic-cache hit mediante `semanticPageReader.current(previous)`. Un rechazo no se convierte en hit. FAST_LISTEN mantiene una guarda por sesión para no invocar indefinidamente el modelo sobre el mismo rechazo; `Procesar` constituye el reintento manual.

## Presentación

Los terminales dicen «preparada», «no pudo interpretarse», «no se pudo procesar» o «cancelada». `completed` mide trabajo terminal; `accepted`, `rejected`, `failed` y `cancelled` miden resultado. El diálogo parcial resume preparados/no interpretables/fallos y reserva causa/clase técnica para la sección expandible.

El aviso de copia de fuente ya no ejecuta `showAndWait` desde un callback de animación: se difiere al siguiente pulso, usa `show()` y entrega la preferencia al cerrarse.

## Verificación

- Suite Maven: 1.280 pruebas vigentes, 0 fallos, 0 errores, 13 smokes opt-in omitidos.
- Regresiones dirigidas: scope PDF, scheduler, FAST_LISTEN, narración incremental, arquitectura semántica, importación DOCX y workspace Word.
- `git diff --check`: sin errores; únicamente avisos LF/CRLF ya propios del working tree.
- Gate físico: se auditó la persistencia real completa. La aplicación no estaba abierta al finalizar, por lo que no se inventó una comprobación manual de clicks posterior al fix.
