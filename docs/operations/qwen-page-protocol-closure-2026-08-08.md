# PAGE_SEMANTIC_READING V1: cierre de generación restringida

Fecha: 2026-08-08  
Estado: **bloqueado; no integrar constrained generation ni avanzar a concurrencia**

## A. Soporte real encontrado

- El ejecutable empaquetado es Ollama `0.32.5`.
- El `llama-server.exe` empaquetado corresponde al build llama.cpp `b4d6c7d8f` y anuncia soporte para `--grammar`, `--grammar-file`, `--json-schema` y `--json-schema-file`.
- El endpoint interno `/completion` de ese `llama-server` acepta los campos `grammar` y `json_schema` por request.
- El código exacto de Ollama `v0.32.5` conserva un campo interno `CompletionRequest.Grammar`, pero su API pública no ofrece una forma de rellenarlo.

Fuentes primarias verificadas: el `--help` de los binarios locales y el código de la etiqueta [Ollama v0.32.5](https://github.com/ollama/ollama/tree/v0.32.5).

## B. Exposición a través de Ollama

Ollama no expone GBNF arbitrario mediante `/api/chat` ni `/api/generate`:

- `ChatRequest` y `GenerateRequest` exponen `format`, no `grammar`.
- `format` solo acepta `"json"` o un objeto JSON Schema.
- `options` no contiene una opción `grammar`; `Options.FromMap` advierte y omite claves desconocidas.

La comprobación contra el ejecutable local usó una gramática que solo permitía `OMEGA` y un prompt que pedía `ALPHA`:

| Variante | HTTP | Resultado | Conclusión |
|---|---:|---|---|
| `grammar` en el nivel superior | 200 | `ALPHA` | campo ignorado |
| `options.grammar` | 200 | `ALPHA` | opción ignorada |

No existe, por tanto, un camino soportado para transportar una gramática distinta por operación desde `QwenVisualAnalysisEngine` al sampler administrado.

## C. Gramática/estrategia ensayada

Se construyó una gramática GBNF mínima y aislada que restringía:

- una sola cabecera `PAGE|V1|idioma|rol`;
- uno o más bloques;
- enum de roles y tipos;
- cuatro coordenadas con forma numérica `0..1000`;
- narratabilidad `N/X/U`;
- confianza con forma numérica `0..1000` (Java seguiría limitándola a `0..100`);
- `SOURCE` no vacío;
- `SPEECH` opcional solo en `MATH`/`IMAGE`;
- `END` por bloque;
- un solo `DONE` final.

La gramática se envió directamente al `/completion` del `llama-server` empaquetado, únicamente como sonda. No se modificó el producto para depender de ese puerto interno.

## D. SOURCE arbitrario

La gramática ensayada permite JSON, código, llaves, corchetes, fórmulas, Unicode y palabras reservadas dentro de líneas ordinarias. Sin embargo, el protocolo V1 sin escape ni longitud es ambiguo cuando el contenido visible contiene una secuencia completa que también es estructural, por ejemplo:

```text
END
BEGIN|PARAGRAPH|10|10|900|100|N|90
SOURCE
```

El parser actual resuelve correctamente palabras y líneas parecidas mediante posición y look-ahead, pero ninguna gramática puede distinguir dos cadenas byte a byte idénticas sin añadir framing. Para libertad total de `SOURCE` hace falta una decisión mínima de protocolo, como *line stuffing* de líneas de control o un payload con longitud. No se añadió ninguna de las dos sin autorización.

## E. Prompt-only frente a constrained

| Caso | Runtime | Perfil | Resultado sintáctico | Datos observados |
|---|---|---|---|---|
| Prompt-only productivo previo | Ollama `/api/chat` | 8K, KV Q8, Flash, AUTO, parallel 1 | inválido | varias páginas y varios `DONE`; el parser estricto rechaza |
| GBNF enviado a Ollama | Ollama `/api/chat` | prueba textual mínima | no aplicado | Ollama ignoró `grammar` |
| GBNF enviado al runner interno | `llama-server /completion` directo | mismo Qwen3-VL 4.4B Q8, 8K, KV Q8, Flash, AUTO, parallel 1 | no completó | timeout a 900 s; 259 tokens decodificados; tarea cancelada |

La sonda directa llegó a aproximadamente `1.73 tok/s` al final de la ventana observada, pero no devolvió una respuesta terminal ni `DONE`; no hay salida publicable ni comparable semánticamente.

## F. Repeticiones y tasa de éxito

No se ejecutaron repeticiones costosas sobre tabla/prosa después del fallo de la sonda base. La tasa de éxito constrained sobre la página obligatoria fue `0/1`. Repetir con la misma frontera y los mismos parámetros no constituye una estrategia materialmente diferente y contradiría la política de retry.

## G. Calidad semántica

No evaluable para constrained: no hubo respuesta completa. La corrida prompt-only previa sí reconoció prosa y celdas, pero su estructura inválida impide construir regiones/bboxes canónicos de forma segura.

## H. Impacto en tokens y tiempo

- Prompt-only previo: alrededor de 491 s en Ollama AUTO y 2.24 tok/s, con salida V1 inválida.
- Sonda GBNF directa: timeout a 900 s, 259 tokens decodificados y sin respuesta terminal.
- No se atribuye toda la diferencia a GBNF: la sonda directa también cruza una frontera HTTP distinta y usa templating manual. El dato sirve para descartar robustez, no como benchmark final de rendimiento.

## I. Cambios implementados

No se modificó código productivo, parser, dominio, persistencia, runtime, Word ni scheduler. Se mantuvieron:

- publicación atómica;
- parser Java estricto;
- `PreparedPdfPage`/`PdfRegion` como autoridad PDF;
- `think=false`;
- Ollama AUTO, contexto 8192, KV `q8_0`, Flash Attention y parallel 1;
- una sola instancia global del scheduler.

Este informe es el único artefacto añadido en este cierre.

## J. Tests

Se ejecutaron 36 pruebas focalizadas con Maven: 36 correctas, 0 fallos, 0 errores y 0 omitidas. El alcance incluyó parser V1, publicación atómica, persistencia de `PreparedPdfPage`, política Qwen, importación/visuales/narratabilidad DOCX, guion de narración y resaltado PDF. El smoke físico constrained se clasifica como timeout/stall y no como éxito parcial.

## K. Regresión Word

No hay cambios en Word ni en operaciones Qwen ajenas a `PAGE_SEMANTIC_READING`. Pasaron las pruebas de importación DOCX básica y avanzada, visuales Word, narratabilidad y construcción del guion de narración.

## L. Smoke físico

- El smoke prompt-only endurecido continúa rechazando el output real inválido.
- La sonda física constrained directa no llegó a `DONE`, no fue parseada ni publicada y terminó por timeout a los 900 s.
- Los procesos temporales Ollama/llama-server fueron cerrados; no quedó runtime huérfano.

## M. Deuda y riesgos

1. Falta una frontera soportada para restricciones textuales por request.
2. El puerto dinámico interno de `llama-server` es detalle privado de Ollama; usarlo desde el producto sería frágil.
3. Un `--grammar` global contaminaría `IMAGE_DESCRIPTION` y otros usos del modelo compartido.
4. Iniciar otro runner exclusivo para PDF cambiaría residencia, lifecycle y frontera de backend.
5. V1 necesita definir cómo representar líneas de payload idénticas a una secuencia completa de control si se exige libertad absoluta.
6. La gramática experimental sufrió timeout/dead-end; requiere trabajo aislado adicional incluso después de resolver el transporte.

## N. Recomendación final

**No. PAGE_SEMANTIC_READING serial V1 aún no está suficientemente estabilizado para avanzar a recursos y concurrencia.**

El bloqueo exacto es doble:

1. Ollama `0.32.5` no expone GBNF arbitrario por request; la única ruta actual cruza una frontera interna no soportada.
2. El protocolo V1 no define framing para una secuencia estructural exacta dentro de `SOURCE`, y la primera GBNF mínima directa terminó en timeout sin una página completa.

La siguiente decisión arquitectónica debe escoger explícitamente entre:

- autorizar una frontera estable de `llama-server` para `PAGE_SEMANTIC_READING` y definir su lifecycle compartido; o
- mantener Ollama y aprobar una evolución mínima del framing V1 que pueda expresarse con una capacidad soportada.

No se recomienda concurrencia hasta resolver y volver a probar ambos puntos.
