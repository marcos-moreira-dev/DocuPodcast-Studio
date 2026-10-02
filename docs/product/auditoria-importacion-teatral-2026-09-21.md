# Auditoría de importación teatral — 2026-09-21

## Causa y corrección

La UI analizaba la gramática, pero preparaba la narración a partir de una segunda importación de Markdown documental. Se perdía el texto hablado del plan teatral y sus índices se asociaban con párrafos que también contenían encabezados, fichas y metadata. Además, el materializador recibía un mapa vacío de recursos.

Ahora la gramática produce una proyección documental propia: un parlamento, un bloque y un segmento con identidad explícita. Los metadatos se conservan como datos estructurados. Los comentarios y ejemplos de código no son diálogo. Markdown documental y la gramática de vídeo narrativo mantienen sus rutas independientes.

Se corrigieron también:

- La resolución de imágenes desde la ubicación original del Markdown y su copia portable al proyecto, con deduplicación y avisos para ausencias o rutas fuera de la obra.
- La asignación de alias oficiales de voz a los IDs reales de la biblioteca.
- La configuración complementaria `config/voces.csv`, incluidos coros declarados, sin inventar voces ni interpretar otros documentos por su nombre.
- La persistencia de límites de escena e identificadores de imágenes, que antes podían cambiar al reabrir.
- Los catálogos visuales de intervenciones y personajes para usar la identidad estructurada y no volver a inferirla del texto hablado.
- La copia de recursos en segundo plano, con actualización del proyecto una vez reunidos los resultados y comprobación de que el usuario no cambió de proyecto durante el trabajo.

## Evidencia

Se auditó el Markdown de compatibilidad de audio de la carpeta de producción facilitada por el usuario, sin modificarlo. La copia generada se guardó fuera de la carpeta original, en `target/patria-import-audit/`.

| Elemento declarado/importado | Cantidad |
|---|---:|
| Parlamentos y segmentos | 557 |
| Personajes | 29 |
| Objetos | 58 |
| Escenas | 15 |
| Imágenes únicas incorporadas | 90 |
| Asignaciones corales | 7 |

Se comprobó la igualdad del texto de todos los parlamentos, la identidad intervención/bloque/segmento, la disponibilidad en la biblioteca de las voces explícitas, los recursos, la serialización del proyecto, la reapertura y la reconstrucción del guion desde su instantánea. No hubo recursos sin resolver en esta entrada.

La batería dirigida pasó 30 pruebas con la obra local habilitada. El empaquetado completo de los seis módulos pasó; en esa ejecución se omitió únicamente la prueba de obra local por ser optativa (29 ejecutadas). No se afirma que se haya ejecutado toda la suite del repositorio.

Registros: `target/theatre-import-validation.log`, `target/theatre-full-build.log` y `target/theatre-stress-validation.log`.

## Archivos de implementación

- `domain/theatre/plan/InterventionPlan.java`: texto hablado e identidad.
- `application/theatre/grammar/TheatreGrammarMarkdownParser.java`: conservación de diálogos y exclusión de comentarios/código.
- `application/theatre/grammar/TheatreGrammarDocumentBuilder.java`: proyección teatral explícita.
- `application/theatre/grammar/TheatreGrammarAssetImporter.java`: resolución y deduplicación de imágenes.
- `application/theatre/grammar/TheatreGrammarVoiceConfiguration.java`: voces y coros complementarios.
- `application/script/BuildNarrationScriptUseCase.java`: reconstrucción de segmentos teatrales conservando la proyección.
- `application/theatre/TheatreImportUseCase.java`: enlaces por identidad, voces e imágenes persistentes.
- `application/grammar/ImportProjectGrammarMarkdownUseCase.java`: configuración y bindings del sidecar.
- `presentation/shell/workflow/GrammarWorkflowCoordinator.java`: ruta de importación y recursos en segundo plano.
- `presentation/shell/DocuPodcastShellViewModel.java`: hidratación de límites de escena.
- `presentation/theatre/IntervencionCatalogo.java` y `TheatreCharacterDetector.java`: consumo de identidad estructurada.
- `application/grammar/TheatreGrammarImportIntegrationTest.java` bajo `src/test/java`: regresión e integración con fixture local optativo.

## Referencia humana y límites

`PATRIA HUMANO V2` se consultó únicamente como referencia de montaje y organización humana. No se importó como si fuera gramática DocuPodcast ni se modificó. Sus necesidades de utilería, continuidad de vestuario, espacialidad, señales técnicas y autoridad entre versiones orientan el [contrato de entrada](../theatre-grammar-format.md), sin introducir condiciones específicas de esa obra en el motor.

La gramática v2 dispone de estado explícito para presencia, objetos, portadores, eventos y vestuario; esos datos deben declararse para poder usarlos. El importador no traduce automáticamente todos los documentos humanos, CSV y JSON a estado escénico. Tampoco se implementó aquí `ACOTACION ... tts=false`, una línea temporal completa de cues LX/SND o la interpretación automática de coreografía.

La validación no incluyó síntesis TTS, mezcla de audio, render de vídeo ni una prueba visual de la aplicación nativa abierta. Se dejó intacta la sesión del usuario; el ejecutable en ejecución requiere reiniciarse con la compilación actualizada para usar los cambios del importador.
