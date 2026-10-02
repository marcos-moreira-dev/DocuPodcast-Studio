# Qwen PAGE_SEMANTIC_READING: comparación controlada Ollama / LM Studio

Fecha: 2026-08-08  
Equipo: NVIDIA GeForce GTX 1650 4 GB, 32 GB RAM, Windows/WDDM  
Carga: `informe-page-02.png`, 1241 x 1754, 326094 bytes

## A–B. Identidad de los modelos

LM Studio 0.4.6 utilizó `qwen/qwen3-vl-4b@q8_0` con llama.cpp CUDA 12
2.13.0:

- lenguaje: `Qwen3-VL-4B-Instruct-Q8_0.gguf`, 4,280,406,048 bytes,
  SHA-256 `BB77131C3FA39A1E8EDCE1BD9A65F49CFAA4502D5FDA7BBEE2671DCCA468F206`;
- projector: `mmproj-Qwen3-VL-4B-Instruct-F16.gguf`, 836,180,160 bytes,
  SHA-256 `1B70E983D69DD424EB16E0ED2104D0B2A58BF01D866657F4DE5DA6C963620F19`.

Ollama 0.32.5 utilizó `qwen3-vl:4b-instruct-q8_0` en un GGUF multimodal
monolítico de 5,088,887,296 bytes, SHA-256
`B39F7405313D33CFB4DAD7E691F5C7A029371058AA6704887BBE143D7BD2E3F3`.

La parte de lenguaje es materialmente equivalente: arquitectura `qwen3vl`, 36
bloques, 4.02 B de parámetros de lenguaje, embedding 2560, 32 cabezas, 8 KV,
vocabulario BPE de 151936 tokens, 398 tensores, Q8_0 (253 tensores Q8_0 y 145
F32), contexto entrenado 262144. La visión también coincide en sus dimensiones
principales: 24 capas, embedding 1024, projector `qwen3vl_merger`, unos 797.43
MiB y 316 tensores en el archivo separado de LM Studio.

No son byte-a-byte idénticos. LM Studio separa lenguaje y `mmproj`; Ollama los
empaqueta y transforma compatiblemente al cargar. Por ello es una comparación
funcional estrecha del mismo modelo/quantización, no un benchmark puro del
mismo archivo físico.

## C. Resultados A/B y puntos de fit

Todos los puntos utilizaron una inferencia, contexto 8192, batch 512,
temperatura 0, seed 42, presupuesto 1800 y la misma carga V1 original.

| Runtime/perfil | KV / Flash | Capas GPU | Projector | Memoria observada | Prompt | Prefill | Salida | Decode | Inferencia | Carga fría / backend | Contrato estricto |
|---|---|---:|---|---|---:|---:|---:|---:|---:|---:|---|
| Ollama AUTO conservador | Q8_0, 612 MiB (136 GPU/476 CPU) / ON | 9/37 | CUDA | pico dedicado observado ~3.10 GiB; pesos 1212 MiB GPU + 3258 MiB host | 2460 | 41.08 s, 59.89 tok/s | 1007 | 450.20 s, 2.24 tok/s | 491.28 s | servidor 8.9 s; smoke 509.1 s | inválido al aplicar parser real |
| LM Studio `--gpu max` | F16, 1152 MiB GPU / ON | 37/37 | CUDA | 3.88 GiB dedicada + ~3.34 GiB compartida; RSS ~7.95 GiB | 2460 | TTFT runtime 55.00 s; imagen 52.34 s | 993 | 240.61 s, 4.12 tok/s | 295.61 s (296.84 s wall) | 13.49 s | inválido |
| Ollama `FIT_TARGET=512` | Q8_0 / ON | 19/37 | CUDA | pico dedicado 3.82 GiB; `size_vram` 3.12 GB | 2460 | 70.92 s, 34.69 tok/s | 1007 | 312.64 s, 3.22 tok/s | 383.56 s | 46.26 s / 430.64 s | inválido |
| Ollama `FIT_TARGET=768` | Q8_0 / ON | 17/37 | CUDA | pico dedicado 3.88 GiB; `size_vram` 2.87 GB | 2460 | 56.80 s, 43.31 tok/s aprox. | 1007 | 361.75 s, 2.78 tok/s | 418.55 s | 62.45 s / 481.18 s | inválido |

En LM Studio el log confirma 4076.43 MiB de pesos CUDA, 394.12 MiB mapeados
en CPU, 1152 MiB de KV F16 CUDA, 301.75 MiB de compute CUDA y 322.49 MiB de
compute visual CUDA. La suma no cabe en 4 GB; `Strict GPU VRAM cap is OFF` y
Windows la sostiene con memoria GPU compartida. El modelo residente responde
sin recarga; se midió una carga fría de 13.49 s.

En Ollama AUTO, el fit reserva explícitamente el peor caso del projector
(estimado en 1363.53 MiB) y margen WDDM: 9 capas, 1462 MiB de uso LLM previsto
y 1829 MiB libres antes de activar toda la ruta visual. Los overrides de 512 y
768 reducen esa reserva; durante visión ambos acabaron cerca de 3.9 GB.

## D–E. Explicación de la diferencia

La cifra manual de 6.53 tok/s de LM Studio correspondía a una imagen sencilla,
447 tokens de salida y unos 24.67 s previos. La página completa redujo LM Studio
a 4.12 tok/s: una caída real causada por la carga más larga y densa, no por un
fallo de Ollama.

La diferencia restante frente a 2.24 tok/s se explica principalmente por el
offload: LM Studio fuerza 37/37 capas y pagina a RAM/WDDM; Ollama deja 9/37 para
reservar VRAM al projector. La KV también difiere: LM usa 1.15 GiB F16 en GPU;
Ollama usa 612 MiB Q8 repartidos, lo cual ahorra memoria pero no compensa 28
capas de lenguaje en CPU. Ambos usan Flash Attention y projector CUDA.

No se puede aislar una cifra exacta atribuible únicamente al runtime porque el
empaquetado físico y las revisiones de llama.cpp difieren. El A/B sí demuestra:

- la página explica parte de la caída de 6.53 a 4.12 tok/s;
- el offload agresivo explica buena parte de la diferencia 4.12 vs 2.24;
- el costo de LM Studio es ~3.34 GiB de memoria GPU compartida y casi 8 GiB de
  working set, además de una salida no aceptable por el parser.

## F–H. Punto Ollama y cambios

El mejor tiempo bruto fue `FIT_TARGET=512`: 383.56 s de inferencia, 22 % menos
que AUTO. No es un default seguro: usó ~3.82 GiB dedicados, dejó del orden de
270 MiB físicos y elevó el prefill de 41.08 a 70.92 s. `FIT_TARGET=768` tampoco
restauró un margen visual razonable y solo redujo la inferencia a 418.55 s.

Se mantiene el AUTO conservador. No se cambió backend, KV, contexto, scheduler,
concurrencia ni política productiva. La única mejora conservada está en el smoke
físico: puede persistir salida bruta, imprime diagnósticos y valida roles, tipos,
bbox, N/X/U y confianza. Esto reveló que el smoke anterior era un falso positivo.

Dos experimentos de prompt posteriores al A/B se descartaron y no quedaron en
producción:

1. gramática explicada sin ejemplo: 2572 prompt, 1436 output, 42.33 s prefill,
   648.52 s decode; produjo etiquetas multilínea y orden `END/SOURCE` inválido;
2. ejemplo concreto: 2605 prompt, 1381 output, 42.65 s prefill, 557.84 s decode;
   repitió `PAGE/DONE`, reutilizó bbox y emitió Markdown.

## I. Desglose de los 1007 tokens

Estimación proporcional sobre la salida original (el tokenizer reportó 1007):

| Categoría | Tokens aproximados |
|---|---:|
| Delimitadores/control | 28 |
| Títulos/encabezados | 29 |
| Prosa SOURCE | 610 |
| Tabla SOURCE, todas sus celdas | 341 |
| SPEECH | 0 |
| Matemática/captions/otros | 0 |

No había duplicación SOURCE/SPEECH que eliminar. La tabla podía ahorrar unas
pocas decenas de tokens retirando barras y separador Markdown, pero el ahorro
sería marginal y la salida seguiría sin bboxes válidos. La salida corta de 1007
tokens no era canónica: agrupó toda la página bajo
`BEGIN|tipo|xMin|...`, que el parser rechaza. Una salida con regiones reales
necesita overhead adicional; no debe calificarse como redundancia.

## J. Pruebas

- `mvn test`: 1204 pruebas, 0 fallos, 0 errores, 11 omitidas.
- Regresión focalizada parser/atomicidad/persistencia/Word/narración/highlight/
  proyección: 37 pruebas, 0 fallos, 0 errores.
- Word: importador básico, avanzado, diagnósticos, narratabilidad y bloques
  visuales pasaron; no se modificó código Word.
- Smoke físico estricto: falla por protocolo aunque la transcripción visual es
  fiel y llega a DONE. El resultado no debe publicarse como PreparedPdfPage.
- LM Studio volvió a quedar sin modelo y con servidor detenido; GPU en reposo
  ~128 MiB.

## K–L. Riesgos y recomendación

Riesgo principal: el modelo reproduce fielmente texto y celdas, pero la carga
V1 original no produce de forma fiable una secuencia que el parser canónico
acepte. `DONE` por sí solo no demuestra validez. Los dos ajustes simples de
prompt empeoraron de maneras distintas, así que no se debe promover ninguno sin
una decisión de diseño y una nueva batería de páginas.

Recomendación: el runtime serial está suficientemente caracterizado para
mantener AUTO/8K/Q8/Flash como baseline seguro, pero el baseline funcional NO
está cerrado y no conviene avanzar aún a concurrencia Qwen. Primero debe
resolverse la producción fiable del contrato V1 (o una estrategia de
constrained decoding compatible), y el mismo smoke debe pasar el parser real
con texto, celdas, bboxes y una sola terminación DONE. Solo entonces tiene
sentido modelar recursos físicos y concurrencia sobre resultados canónicos.
