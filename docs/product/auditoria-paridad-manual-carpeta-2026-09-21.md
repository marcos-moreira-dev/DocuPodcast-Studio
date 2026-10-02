# Auditoría: configuración manual frente a carpeta determinista

Estado posterior: véase el [cierre de correcciones del 22 de septiembre](cierre-paridad-carpeta-teatral-2026-09-22.md). Este documento conserva los hallazgos y fallos tal como fueron observados durante la auditoría original.

## Dictamen

**La equivalencia solicitada todavía no se cumple.** Hay piezas deterministas reutilizables y una gramática teatral capaz de representar bastante información, pero no una única importación de carpeta que reproduzca íntegramente la configuración manual y prepare el documento de parlamentos sin depender de lo que ya estaba abierto.

La comprobación anterior de 557 parlamentos validaba la ruta de gramática y su persistencia. No acreditaba la paridad de la ruta de carpeta oficial. Esta auditoría identifica esa diferencia y reproduce fallos que los tests anteriores del paquete no detectaban.

Alcance: configuración teatral, texto, voces, recursos visuales, audio y exportación/importación de paquetes. Se inspeccionaron código, captura proporcionada y archivos guardados; no se manipuló la aplicación nativa abierta. No se certificaron todos los modos de DocuPodcast, ni se ejecutaron motores de IA, TTS o vídeo.

## Hallazgos prioritarios

### P1 — La carpeta oficial no produce su propio documento y guion

`TheatrePackageRefreshCoordinator.startOfficialImport` pasa `currentScriptProperty()` al importador y, al finalizar, aplica únicamente el proyecto. No adjunta un documento nuevo ni reemplaza la lectura desde el guion del paquete.

`ImportOfficialTheatrePackageUseCase.execute` analiza la gramática, pero usa ese guion externo para construir las intervenciones y sus snapshots. Al importar una carpeta válida en un proyecto sin guion, el texto del snapshot queda vacío aunque el Markdown contiene un parlamento.

**Reproducción:** esperado `Este es el parlamento del paquete.`; obtenido texto vacío. El resultado depende del estado previo de la aplicación. La solución debe hacer que carpeta y Markdown entren por el mismo constructor de documento/guion teatral.

### P1 — Audio presente en el catálogo no implica audio aplicado al render

`ReconcileTheatreProjectUseCase.upsertHumanAudio` guarda el rango con `intervention.blockId()` como si fuera un ID de segmento. El render compara con el ID real del segmento. Con la proyección nueva, `B-INTERVENCION-1` y `SEG-INTERVENCION-1` son identidades distintas.

**Reproducción:** un `HUMAN_AUDIO` válido como referencia del paquete produce `TEXT_TO_SPEECH` en el plan en lugar de `AUDIO_CLIP`. La prueba comprueba la selección de fuente de audio; no decodifica el archivo.

El exportador presenta la misma confusión al localizar audio humano por `blockId == textRange.segmentId`. Es necesario resolver todos los enlaces mediante la relación intervención → bloque → segmento, incluyendo rangos dentro del parlamento.

### P1 — Exportar y reimportar no conserva toda la configuración

**Reproducción:** `aplicar_plano=false` pasa a `true` al exportar y analizar el Markdown resultante. `TheatreGrammarV2Writer` no serializa esa política. Por inspección tampoco escribe el contexto de intervención ni todos sus cambios de fondo, y omite intervenciones que no tienen una colocación `TextActionPlacement` en una escena.

Además, `ExportOfficialTheatrePackageUseCase.bindings` conserva un solo binding por asset mediante `putIfAbsent`. Deduplicar el archivo es correcto; deduplicar sus usos no lo es. Los usos que no queden representados también en la gramática pueden perderse.

### P2 — Hay funciones manuales sin contrato equivalente de carpeta

| Función | Cobertura de carpeta actual |
|---|---|
| Actos, escenas, personajes, objetos | Representables en gramática; falta integrar la creación completa del documento |
| Límites de escena | Se calculan; la ruta oficial no aplica los mapas de inicio/fin al estado visual |
| Imágenes, mapas y fondos | Bindings básicos disponibles; falta garantizar todos los usos y cambios de fondo en la ida y vuelta |
| Voz existente por personaje | Hay asignación; la ruta oficial no incorpora el complemento `config/voces.csv` de la ruta Markdown |
| Referencias de voz aportadas | `VOICE_SAMPLE` se registra en catálogo, sin binding explícito a un perfil/set de voz |
| Coros | Se representan participantes; no equivale a transportar y restaurar la mezcla y su estado completo |
| Audio humano | Hay declaración, pero falla su vínculo al render en la reproducción descrita |
| Pistas de audio manuales | La UI admite recorte, volumen, modo de finalización y fundido; el exportador no serializa `theatre.audioTracks` |
| Cámara y contexto | Parcial; pérdida reproducida de política de aplicación de cámara |
| Presencia, portadores, entradas/salidas, vestuario | Estado v2 representable; no certifica todas las operaciones de edición manual |
| Vídeo aportado | `VIDEO` se registra en catálogo, sin asignación funcional explícita |

Fuentes principales: `TheatreAudioTrackWorkflow.save`, `ReconcileTheatreProjectUseCase.reconcile`, `TheatreGrammarV2Writer.writeScene` y `ExportOfficialTheatrePackageUseCase.bindings`.

### P2 — El contrato del manifiesto y la implementación discrepan

El escáner permite declarar `grammar` y valida esa ruta, pero el importador abre siempre `obra.teatro.md`. La ruta declarada debe ser la única autoridad.

La misma carpeta oficial importada de nuevo falla cuando ya existe su directorio de destino. No tiene la misma semántica idempotente de la actualización incremental de schema 1. Importar sin cambios debería ser una operación sin cambios, o exponer claramente un modo diferente de importación, no fallar tras presentarse como refresco.

### P2 — El ejemplo de producción tampoco es un manifiesto oficial v2 completo

El `docupodcast-theatre.json` de la carpeta de producción facilitada contiene `schemaVersion`, `title`, `source` y `assets`; carece de `packageId` y `grammarVersion`, obligatorios para el escáner oficial v2. Esto es una diferencia del artefacto de entrada, adicional a los bugs del producto. Añadir campos al ejemplo no corrige los fallos de paridad del motor.

`PATRIA HUMANO V2` no se considera formato de entrada. Sus instrucciones y documentación se utilizan exclusivamente como referencia humana.

## Por qué la captura sigue mostrando detalles técnicos

El documento guardado del proyecto señalado tiene **36 bloques**, de los cuales **29** contienen `origen=`, `mapa_espacial=` o `fondo_escenario=` en su texto. La copia corregida tiene **557 bloques**, sin esos tokens técnicos en el texto. La captura es coherente con la instantánea antigua, no con la copia corregida.

Corregir el importador no migra por sí solo un documento ya persistido. La ruta de carpeta oficial tampoco sustituye ese documento. Debe existir una actualización explícita que preserve las ediciones del usuario y regenere la proyección desde una fuente teatral identificada; ocultar cadenas con CSS o filtrar palabras técnicas no resuelve la asociación de datos.

La vista tipo Word debe consumir exclusivamente `spokenText`. Identidad del hablante, escena, voz y demás configuración pueden mostrarse en etiquetas o paneles, pero no concatenarse al parlamento ni convertirse en texto para TTS. El mismo invariante debe mantenerse al abrir, guardar, refrescar y volver a generar la lectura.

## Pruebas y evidencias

Se ejecutaron seis tests existentes de `OfficialTheatrePackageTest`: **6 pasaron**. Se añadieron tres sondas de equivalencia: **3 fallaron con aserciones concretas, sin errores de ejecución**. El fallo de Maven en esta auditoría es el resultado esperado de las brechas reproducidas, no un error de compilación.

Los tests existentes comparan principalmente snapshots estructurales y usan guion nulo en sus casos de ida y vuelta; por eso pueden pasar sin conservar el texto hablado ni probar la selección efectiva de audio.

Se conservaron [las sondas](../audits/theatre-folder-parity/TheatreFolderParityAuditTest.java) y [sus resultados](../audits/theatre-folder-parity/resultados.txt) fuera de la suite predeterminada. Para repetirlas, copiar la clase temporalmente al paquete equivalente bajo `src/test/java` y ejecutar:

```powershell
mvn -pl studio-desktop -am test '-Dtest=TheatreFolderParityAuditTest,OfficialTheatrePackageTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

Registro completo: `target/theatre-folder-parity-audit.log`. Esta auditoría no modifica código de producto ni el proyecto del usuario.

## Contrato recomendado para cerrar la brecha

Conservar una raíz versionada `docupodcast-theatre.json` como índice declarativo de la carpeta. No debe ser un texto libre con instrucciones para una IA. Debe referenciar el guion, identidades, configuración y assets con campos validados. Se puede mantener la gramática actual como representación legible sin mantener un segundo motor de importación.

Flujo único: **validar carpeta → construir modelo completo → resolver referencias → generar documento de parlamentos y guion → aplicar el proyecto conjuntamente → guardar**. El montaje de datos no requiere IA; la síntesis o generación de medios, cuando se solicite, es una operación posterior.

Orden de corrección:

1. Unificar la importación de carpeta y Markdown, eliminando la dependencia del guion previamente abierto.
2. Corregir identidades de audio y conservar límites de escena; añadir migración/reimportación controlada de documentos antiguos.
3. Completar la serialización de políticas, contexto, fondos, pistas, muestras de voz y todos los bindings.
4. Respetar la ruta del manifiesto y definir refresco idempotente y política de conflicto con cambios manuales.
5. Exigir paridad verificable: configurar manualmente → exportar → importar en proyecto vacío → comparar estado funcional y planes efectivos de reproducción/render, además del texto limpio. Usar fixtures propios pequeños y obras grandes externas como pruebas de estrés.

La aceptación no es «aparece en la ficha»; es **queda configurado, se conserva al reabrir y lo utiliza la reproducción/render igual que cuando se configura manualmente**.
