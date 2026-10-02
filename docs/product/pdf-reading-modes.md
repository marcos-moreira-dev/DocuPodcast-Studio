# Modos de lectura PDF

DocuPodcast Studio separa el texto visible del texto narrable. Abrir una página no ejecuta IA local y ningún resultado automático entra en la voz hasta que una persona lo aprueba.

## Preferencia de extracción por documento

En **Índices y preferencias del documento**, el selector **Cómo leer este PDF**
guarda la preferencia del PDF sin iniciar trabajo. Solo aparece para fuentes PDF;
el módulo de índices permanece disponible para Word y las demás fuentes.

- **Lectura directa**: extrae el texto nativo con PDFBox, sin OCR ni análisis
  generativo. Es la opción predeterminada para PDF nuevos. Una página sin texto
  extraíble informa la limitación sin impedir preparar otras páginas del alcance.
- **Lectura con reconocimiento**: utiliza primero el texto nativo y recurre al OCR
  existente cuando hace falta. Requiere tener disponible ese motor para páginas escaneadas.
- **Lectura con interpretación**: conserva la ruta de análisis semántico local.

Los manifiestos anteriores sin preferencia explícita mantienen la ruta de
interpretación. Cambiar la preferencia no borra páginas, revisiones ni audios;
al preparar se verifica la compatibilidad de la caché. Las correcciones que no
puedan reconciliarse conservan la página anterior y requieren revisión.
Poppler se conserva como adaptador alternativo (`nativeTextProvider`); PDFBox
está integrado y no requiere instalar Poppler. Los nombres técnicos no aparecen
en el selector de lectura del estudiante.

## Lectura esencial

- Lee prosa fiable y mantiene el resaltado sincronizado.
- Omite ruido OCR, fórmulas crudas y objetos sin descripción aprobada.
- No requiere Qwen, reconocimiento matemático ni análisis avanzado de tablas.
- Es el modo recomendado cuando se quiere empezar a escuchar pronto o el equipo tiene recursos limitados.

## Documental inteligente

- Conserva la prosa fiable como primera salida audible.
- Prepara figuras, tablas y fórmulas localmente cuando existe el motor correspondiente.
- Toda salida automática se guarda como borrador. La falta de un motor degrada solo el objeto afectado.
- Una tabla numérica se resume con cifras verificables; una tabla textual pequeña puede leerse de forma estructurada.

## Revisión avanzada

- Expone evidencias, regiones, borradores, fallos, truncamientos y contenido omitido.
- Permite corregir celdas, elegir filas o columnas, definir descripciones manuales y aprobar o rechazar.
- La aprobación por página o documento siempre muestra una previsualización y exige una acción explícita.

Los modos son presets. La política efectiva sigue siendo editable por objeto: por ejemplo, Lectura esencial puede mantener el resto de tablas omitidas y preparar una tabla concreta con `SUMMARIZE`.

## Capacidades y requisitos

La interfaz informa capacidades funcionales —OCR, descripción visual, tablas avanzadas, reconocimiento y voz matemática— sin guardar detalles del hardware en el proyecto. El mismo proyecto puede abrirse en otra máquina: los derivados terminados se conservan y las capacidades ausentes aparecen como degradación visible.

En el perfil conservador, OCR, análisis generativo y TTS comparten el planificador de recursos; Qwen y TTS se serializan. En un perfil estándar se permite más prefetch de páginas, pero la identidad del proyecto y sus decisiones de revisión no cambian.

## Estados operativos

Cada intento derivado queda auditado como `COMPLETED`, `FAILED`, `CANCELLED`, `TRUNCATED` o `INSUFFICIENT_EVIDENCE`. Se registran duración, TTFT, tokens, contexto, RAM/VRAM/CPU disponibles, motor, modelo, resolución y `done_reason`. Al reanudar se reutiliza un resultado cuyo fingerprint de evidencia y parámetros siga vigente, evitando duplicados.

## Criterio de seguridad

- `DRAFT`, `REJECTED`, `STALE`, `UNKNOWN`, `UNCERTAIN` y matemática OCR cruda no llegan al TTS.
- MathCAT solo recibe MathML validado.
- Qwen no decide geometría ni se aprueba a sí mismo.
- Los tratamientos manuales aprobados y vigentes sí pueden narrarse.

## Comparación local optativa

`PdfNativeAdapterComparisonTest` se habilita con `-Dpdf.compare.poppler=true` y
requiere `pdftotext` en PATH. Compara texto y límites de coordenadas sin modelos;
escribe tiempos y muestras del heap JVM en `target/pdf-native-comparison.csv`.
No mide el pico de memoria de cada motor. El PDF histórico de smoke tiene una
tabla de referencias dañada: la prueba utiliza un PDF nativo controlado temporal,
sin modificar el ejemplo original. Esta comparación no sustituye una prueba con
el PDF real del usuario.
