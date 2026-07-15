## FIRST-USE-ONBOARDING1 — implementada

Inicio y Configuración inicial quedan orientados a escuchar rápido con Voz local simple. La guía integrada habla de audio final y video MP4 como flujo normal. Ver `docs/productizacion/FIRST_USE_ONBOARDING1_ESCUCHAR_RAPIDO.md`.

## DOCUMENT-SIDEBAR-VOICE-UX1 — implementada

Documento distingue Voz generada y Audio del computador; el campo visible queda como Tono y el ComboBox muestra solo tonos simples realmente registrados para la voz elegida. Ver `docs/productizacion/DOCUMENT_SIDEBAR_VOICE_UX1_VOZ_IA_AUDIO_LOCAL.md`.

## 7. VOICE-LIBRARY-SYNC1 — implementada

Las muestras de referencia de voces viven en `voice-library/samples/` de la app/runtime. El proyecto conserva la biblioteca y las referencias de voz/tono, pero no copia esos WAV a la carpeta de cada proyecto. Documento se actualiza al cambiar `activeVoiceLibraryProperty()` y filtra tonos por los realmente registrados para la voz elegida.

# Tandas 01–11 — bloque funcional y UX principal

> Nota: el nombre del archivo conserva la numeración histórica `01_09`, pero el contenido queda actualizado tras dividir el bloque de voces. El bloque funcional principal ahora llega hasta la tanda 11.

Este bloque corrige los problemas que el usuario percibe directamente al usar la app: voz avanzada, GPU, índice/reproducción, velocidad, Vista Voces, registro de muestras, Documento, primera experiencia, readiness de exportación y Configuración.

---

## 1. MOTOR-ADV-READY-GATE1 — Voz IA avanzada: descargada vs usable

**Estado:** implementada. Documento operativo: `docs/productizacion/MOTOR_ADV_READY_GATE1_USABLE_REAL.md`.

### Objetivo

Impedir que Voz IA avanzada se use para generar chunks cuando solo está descargada/verificada, pero no probada con audio real.

### Reglas

- `Descargado` no equivale a `usable`.
- `READY_WITH_WARNINGS` no basta para documentos largos con Voz IA avanzada.
- Debe existir prueba WAV generada antes de permitir generación de chunks con motor avanzado.
- El mensaje debe ser humano: `Voz IA avanzada está descargada, pero todavía no está probada para generar documentos`.

### Criterio de salida

La app no inicia generación de chunks largos con Voz IA avanzada si no hay evidencia de prueba WAV real.

---

## 2. MOTOR-GPU-SMOKE1 — GPU real dentro del Python autocontenido

**Estado:** implementada. Documento operativo: `docs/productizacion/MOTOR_GPU_SMOKE1_CUDA_RUNTIME_LOCAL.md`. Hotfix posterior: `MOTOR-GPU-SMOKE1-HF2` corrige el probe para ejecutarlo mediante archivo `.py` temporal.

### Objetivo

GPU solo es usable si el runtime Python local confirma CUDA.

### Reglas

- GPU detectada por Windows = candidata.
- GPU con `torch.cuda.is_available() == true` = usable.
- Si CUDA no existe en el venv, mostrar: `GPU detectada por Windows, pero no disponible para Voz IA avanzada en este runtime Python`.
- No pasar `cuda:0` sin smoke real.

### Criterio de salida

La UI y el gateway no se contradicen: si el motor usa CPU, lo dice; si usa GPU, hay evidencia.

---

## 3. DOC-INDEX-PLAYBACK-HF1 — índice → generar/reproducir desde selección

**Estado:** implementada. Documento operativo: `docs/productizacion/DOC_INDEX_PLAYBACK_HF1_GENERAR_DESDE_INDICE.md`.

### Objetivo

El índice seleccionado debe convertirse en pivote real de generación/reproducción.

### Regla

Documento largo → clic en índice → reproducir → genera/reproduce desde ese bloque, no desde el inicio.

---

## 4. PLAYBACK-SPEED-HF9 — 1.5x/1.75x sin silencio artificial

**Estado:** implementada. Documento operativo: `docs/productizacion/PLAYBACK_SPEED_HF9_TIMING_REAL.md`.

### Objetivo

Avanzar al siguiente chunk cuando termina el WAV acelerado.

### Regla

A `1.75x`, el siguiente chunk inicia al terminar el audio acelerado, sin sensación de espera `1x`.

---

## 5. VOICE-UX-POLISH1A — implementada: limpieza operativa de Vista Voces

### Hallazgo de lectura y feedback visual

Vista Voces está funcional, pero tiene fricción:

- botón `Ver estado de voces` sin función clara;
- `Resumen de operación` duplica información;
- microcopy de motor/dispositivo con color casi invisible;
- estado `Sin prueba generada` parece botón o microcontenedor accionable;
- lista de voces no muestra de forma suficientemente útil si una voz es simple o avanzada ni qué emociones tiene;
- algunos textos explican demasiado.

### Para qué sirve Vista Voces

Vista Voces sirve para administrar la biblioteca de voces y muestras. Documento es el lugar donde esas voces se asignan a fragmentos.

Debe quedar claro:

```text
Voces administra voces y muestras. Documento asigna voces a fragmentos.
```

### Cambios esperados

- Mantener título `Inicio` y microcopy operativo.
- Quitar `Ver estado de voces` si no ejecuta acción real.
- Quitar `Resumen de operación` si solo duplica estado del módulo `Configurar motor`.
- En `Biblioteca de voces`, listar voces como entidades operativas:
  - `Narrador prediseñado · Voz simple`
  - `María · Voz avanzada`
  - tags: `Neutral`, `Feliz`, `Triste`, solo si existen.
- No mostrar emociones no asignadas como tags.
- Corregir color del microcopy oculto/blanco.
- `Sin prueba generada` debe ser estado textual, no botón.
- `Abrir configuración completa` debe usar estilo de acción coherente.

### Reglas de UI

- Nada de dashboard decorativo.
- Nada de tarjetas sin función.
- Cada región debe permitir entender, configurar, probar, registrar o validar.

### Tests/gates

- No aparece `Ver estado de voces` sin handler real.
- No aparece `Resumen de operación` si no tiene operación propia.
- No hay microcopy blanco ilegible.
- Los tags de voz muestran solo emociones asignadas.

### Criterio de salida

Vista Voces queda más simple y profesional sin introducir grabación todavía.

---

### Estado aplicado en código

Estado aplicado en código: fuera `Ver estado de voces`, fuera `Resumen de operación`, filas de voz con tipo humano y tags de tonos registrados, estado de motor legible, `VoiceToneLabelPolicy` para ComboBox simples y estado de prueba sin apariencia de botón.

## 6. VOICE-REGISTRATION-WIZARD1 — crear voz con muestras de referencia

### Objetivo

Implementar una subvista real para crear una voz y grabar muestras por emoción/tono.

### Regla conceptual

Las muestras no son clips finales para reproducir siempre igual. Son referencias para que Coqui/XTTS genere texto nuevo con esa voz y tono.

Ejemplo:

- `María · Neutral` sirve para narración neutral.
- `María · Enojada` sirve para que la IA genere cualquier frase futura con esa referencia.

### Flujo UX

Desde `Gestionar voces`, el botón `Nueva voz` abre una subvista:

```text
Gestionar voces → Nueva voz
```

Debe existir:

```text
← Volver a gestionar voces
```

### Campos

- Nombre de la voz.
- ComboBox de emoción/tono con labels simples.
- Frase sugerida para actuar.
- Estado de grabación.
- Estado de muestra asignada.

### Acciones

- `Grabar`.
- `Detener`.
- `Reproducir grabación`.
- `Eliminar grabación`.
- `Asignar a este tono`.
- `Guardar voz`.

### Captura de voz

La grabación debe hacerse con Java/API de audio. No usar Python para capturar micrófono.

Python/Coqui/XTTS queda para sintetizar usando la muestra como referencia.

### Almacenamiento

Las muestras se guardan en la biblioteca de voces de la app/runtime/repositorio local, no dentro de cada proyecto.

### Regla obligatoria

No se puede guardar una voz avanzada sin muestra `Neutral`.

### Criterio de salida

El usuario puede crear `María`, grabar `Neutral`, opcionalmente agregar `Feliz` o `Triste`, y guardar una voz usable.

---

## 7. VOICE-LIBRARY-SYNC1 — sincronización de biblioteca con Documento

### Objetivo

Documento debe ver inmediatamente las voces y tonos registrados sin reiniciar la app.

### Cambios esperados

- Al guardar una voz, se refresca la biblioteca activa.
- Vista Voces actualiza lista y tags.
- Sidebar de Documento actualiza ComboBox de voces.
- Al elegir una voz, Documento actualiza ComboBox de tonos con sus muestras reales.
- Si se elimina una muestra, ese tono desaparece del ComboBox de Documento.

### Regla clave

Si `María` solo tiene `Neutral`, `Feliz` y `Triste`, Documento solo muestra esos tonos. `Sorprendido` no aparece hasta que exista una muestra `María · Sorprendido`.

### Criterio de salida

La biblioteca de voces es transversal: lo configurado en Voces se puede usar en Documento de forma inmediata y honesta.

---

## 8. DOCUMENT-SIDEBAR-VOICE-UX1 — sidebar de Documento: voz IA vs audio local

### Objetivo

Pulir el sidebar de Documento cuando la biblioteca de voces ya sincroniza.

### Regla funcional

Para cada fragmento, el usuario puede elegir:

1. Voz IA generada desde una voz/tono registrado.
2. Audio local del computador.

### Cambios esperados

- Labels simples: `Voz`, `Tono`, `Audio`.
- ComboBox de tono solo con nombres simples.
- No usar `fallback` en UI común.
- Si falta una emoción, mensaje humano: `No hay muestra “Enojado”; se usará Neutral.`
- Voz local simple no muestra emociones.
- Voz IA avanzada solo muestra tonos registrados para la voz seleccionada.

### Criterio de salida

El usuario entiende que puede generar voz IA o usar audio propio, sin confundir muestras de referencia con clips finales.

---

## 9. FIRST-USE-ONBOARDING1 — primera experiencia orientada a escuchar rápido

### Objetivo

Que un usuario nuevo escuche rápido un documento.

### Regla UX

Primer uso recomendado:

1. Preparar Voz local simple.
2. Abrir documento.
3. Escuchar.
4. Luego mejorar con Voz IA avanzada si quiere calidad superior.

### Criterio de salida

El usuario puede probar la app sin descargar primero un modelo pesado.

---

## 10. EXPORT-READINESS-UX1 — estado humano antes de exportar

### Objetivo

Mostrar antes de exportar qué está listo y qué falta.

### Estados esperados

- Audio WAV final: listo/bloqueado.
- MP3/AAC: listo/requiere Video local.
- MP4 final: listo/faltan imágenes o audio/requiere Video local.
- Paquete auditable: soporte avanzado.

### Criterio de salida

El usuario no descubre tarde que no puede exportar.

---

## 11. SETTINGS-RUNTIME-UX1 — configuración de motores por pasos humanos

### Objetivo

Ordenar la UX sin rediseño total.

### Orden humano para Voz IA avanzada

1. Preparar.
2. Probar.
3. Reproducir prueba.
4. Usar.

Descargar/importar quedan como opciones secundarias.

### Criterio de salida

Configuración distingue preparado, probado y usable; `listo con advertencias` no parece igual que `listo`.


## VOICE-REGISTRATION-WIZARD1 implementada

Nueva voz abre una subvista dedicada con nombre, tono simple, frase guía, importar/grabar/detener/asignar/reproducir/eliminar y Guardar voz con Neutral obligatoria.
