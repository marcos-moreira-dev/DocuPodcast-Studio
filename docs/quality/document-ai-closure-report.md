# Informe de cierre de IA local documental

Fecha de verificación: 28 de julio de 2026.

## Estado

Las Tandas 18–26 quedan implementadas en código y cubiertas por pruebas
automatizadas. Qwen Q4 y Q8 cuentan con evidencia física real. La capacidad
PP-StructureV3 permanece deliberadamente en estado `DEGRADED` hasta importar
un paquete portable con manifiesto, tamaños y SHA-256 verificables. No se
declara `READY` por mera presencia de archivos.

Los motores de análisis son transversales y neutrales. Durante estas tandas,
sus acciones visibles se ofrecen únicamente desde Estudio documental. No se
añadieron consumidores en Teatro, video, probadores ni generación de imágenes.

## Superfiltro de narratabilidad

La acción manual «Revisar galimatías de esta página» envía a Qwen una sola
solicitud textual con todas las regiones de la página y sus IDs estables. La
respuesta propone, para cada región:

- `NARRATABLE`;
- `NON_NARRATABLE`;
- `UNCERTAIN`;
- razón normalizada y confianza.

El resultado se guarda como `DRAFT`. Aprobarlo modifica exclusivamente el
override de narratabilidad. Nunca elimina ni reescribe el texto canónico, la
geometría, las evidencias nativa/OCR o los índices de búsqueda. Un fragmento
`UNCERTAIN` continúa fuera del TTS hasta una decisión humana.

La revisión se reutiliza mientras no cambie la revisión efectiva de la página.
No se ejecuta al abrir, buscar o reproducir un documento.

## Evidencia física Qwen

Runtime administrado: Ollama standalone 0.32.5, loopback privado, puerto
efímero y contención mediante Windows Job Object con `KILL_ON_JOB_CLOSE`.

Modelo Q8:

- tag: `qwen3-vl:4b-instruct-q8_0`;
- digest: `3c4e71051a81b489293105e076ca5f2d4ab03bfeba64f01fb041958c5adb0b3a`;
- prueba visual real correcta sobre la página 895 de Burden;
- VRAM efectiva informada por `/api/ps`: 1.860.991.712 bytes;
- revisión de seis regiones: 89.187 ms en frío y 73.491 ms caliente.

Modelo Q4:

- tag: `qwen3-vl:4b-instruct-q4_K_M`;
- digest: `ee4b975b58c17ce268cd19d40db35d5edc64603035d2ffc1fee1968eb0947f7b`;
- prueba visual real correcta sobre la misma página;
- VRAM efectiva informada por `/api/ps`: 1.857.646.755 bytes;
- revisión de seis regiones: 60.645 ms en frío y 48.974 ms caliente.

En ambos perfiles, la prueba de narratabilidad conservó tres regiones de prosa,
excluyó dos regiones estructurales y dejó una región ambigua como `UNCERTAIN`.
Q4 es una elección económica explícita; nunca reemplaza silenciosamente a Q8.

Los certificados se almacenan fuera de los proyectos, vinculados al runtime,
modelo y huella de hardware:

- `state/document-ai-certifications/qwen3-vl-local--qwen3-vl_4b-instruct-q8_0.properties`;
- `state/document-ai-certifications/qwen3-vl-local--qwen3-vl_4b-instruct-q4_K_M.properties`.

## Recursos y fallos

- La inspección profunda valida manifiesto y todos los blobs de Qwen.
- El preflight ocurre antes de acceder a red y comprueba espacio con margen.
- Un recurso válido se reutiliza o exige confirmación explícita para
  redescargarse.
- PP-Structure rechaza `.venv`, `pyvenv.cfg`, rutas absolutas y descargas
  internas de Paddle.
- Están cubiertos puerto ocupado, carrera de puerto, proceso hijo, timeout,
  cancelación, salida prematura, OOM, falta de espacio, recurso corrupto,
  staging y cierre abrupto.
- Un OOM no cambia silenciosamente de GPU a CPU.
- Los resultados válidos y el PDF original sobreviven a cualquier fallo.

## Corpus

Los originales del Escritorio fueron revalidados sin copiarlos ni
modificarlos:

- Kress, 342 páginas:
  `2631606a9815ebd76573160f825d6be0e95fd35c3e731fa0abd52671cbf20cdf`;
- Burden, 895 páginas:
  `c5af17c41cc4049f9b3f028ad513182d316d6d1c7a1d683250fb18bda2e48d6d`.
- De los subconjuntos a la poda, 118 páginas digitales:
  `e1d79ff6607cedbd0381ccf9ce8f472e542e5013588e6ca8ed3055852022fccf`.

El tercer documento añade tablas de verdad, matrices, fórmulas, cuadros,
recurrencias y comparaciones de estrategias. Las páginas 14, 19, 49, 54, 73,
82, 93, 97 y 102 quedan registradas y renderizadas en el harness real.

No se repitió el recorrido completo de 1.237 páginas porque esta entrega no
modificó extracción, reconciliación, layout canónico ni persistencia. Se
conserva la línea base completa ya verde y se revalidaron ambos hashes.

## Puerta pendiente

Para declarar toda la característica `READY` falta exclusivamente la
certificación física de PP-StructureV3 con un paquete portable distribuible o
importado que pase su manifiesto verificable. Hasta entonces:

- Tesseract continúa como ruta estándar local;
- Qwen Q4/Q8 y MathCAT pueden certificarse y utilizarse;
- análisis avanzado PP permanece visible como «Importar análisis avanzado»;
- no existe descarga ni instalación de fachada.

## Verificación final

- Suite Maven completa: 1.042 pruebas, 0 fallos, 0 errores y 10 omisiones
  condicionadas.
- Arquitectura, política de componentes JavaFX y control UTF-8 incluidos en la
  suite verde.
- Cero procesos `ollama` o `llama-server` después de las pruebas.
- Cero listeners residuales en los puertos privados usados por las
  certificaciones.
- SHA-256 de Kress y Burden coincidentes con el registro del corpus.
- SHA-256 de `De_los_subconjuntos_a_la_poda.pdf` coincidente con el registro.

## Tandas 23–26 — tablas y orquestación de cómputo

### Tanda 23 — cola global

- `PriorityResourceScheduler` concede CPU, GPU y memoria de modelo de manera
  atómica, por prioridad y FIFO.
- Audio crítico, audio manual, análisis interactivo, anticipación y preparación
  de fondo tienen prioridades explícitas; el envejecimiento de fondo nunca
  supera audio crítico.
- La cola es visible y cancelable desde Motores y dependencias.
- XTTS dispone de un worker persistente por concesión. Termina un chunk antes
  de ceder, publica inmediatamente cada WAV válido y descarga el modelo antes
  de liberar la concesión.
- Qwen usa `keep_alive=0`, consulta `/api/ps` y confirma la liberación antes de
  que otro motor pesado pueda adquirir la memoria.
- La capacidad predeterminada continúa siendo un único modelo pesado.

### Tanda 24 — tablas académicas

- Las tablas derivadas contienen celdas estables, fila, columna, spans,
  geometría, encabezados, caption, unidades, tipo y confianza.
- La clasificación distingue prosa, clave/valor, mezcla, datos numéricos,
  matrices, matemáticas y desconocido.
- Mínimos, máximos, variaciones y monotonicidad son deterministas.
- Qwen recibe únicamente estructura, estadísticas y contexto cercano. Una
  cifra o unidad sin respaldo, o un `cellId` inexistente, invalida el borrador.
- Las tablas grandes no se recitan automáticamente. El usuario puede elegir
  filas y columnas; si completa ambos campos se usa su intersección.
- La UI permite reconocer estructura avanzada con PP, corregir celdas,
  preparar lectura determinista y solicitar resumen contextual.
- PP y Qwen generan siempre derivados `DRAFT`; no modifican texto, geometría,
  orden ni narratabilidad canónicos.
- El foco de audio usa las geometrías de las celdas efectivamente citadas.

### Tanda 25 — política estricta Qwen

- Q8 es el perfil de calidad automático; Q4 requiere una elección persistida y
  muestra advertencia de menor fidelidad.
- No existe fallback silencioso Q8→Q4 ni GPU→CPU.
- Cuando se solicita GPU, VRAM cero en `/api/ps` produce
  `DEVICE_MISMATCH`.
- La revisión de galimatías exige exactamente una decisión por ID conocido.
  IDs inventados se rechazan y decisiones faltantes, duplicadas,
  contradictorias o de baja confianza se convierten en `UNCERTAIN`.
- Qwen nunca recibe el JSON de página. Recibe representaciones compactas de
  regiones; las páginas largas se fragmentan conservando contexto común y no
  se publica nada si falla un fragmento.

### Tanda 26 — evidencia física

El 28 de julio de 2026 se repitió la puerta visual real aislada:

- Q8: 256,4 s, 1.860.991.712 bytes de VRAM informada, modelo liberado;
- Q4 manual: 230,0 s, 1.857.646.755 bytes de VRAM informada, advertencia
  persistida y modelo liberado;
- tras ambas pruebas: cero procesos Java del smoke, Ollama o llama-server y
  cero listeners asociados.

La suite completa terminó con 1.042 pruebas, 0 fallos, 0 errores y 10
omisiones opt-in. PP-Structure permanece correctamente `DEGRADED`: el bridge,
manifiesto, preflight, importación, operaciones de tabla y pruebas simuladas
están cerrados, pero falta importar o distribuir un paquete portable físico
para certificar CPU/GPU sobre el corpus.
