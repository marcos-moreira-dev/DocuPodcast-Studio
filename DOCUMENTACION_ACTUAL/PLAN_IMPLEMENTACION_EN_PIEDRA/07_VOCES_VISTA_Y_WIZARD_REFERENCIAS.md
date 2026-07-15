## DOCUMENT-SIDEBAR-VOICE-UX1 — implementada

Documento distingue Voz generada y Audio del computador; el campo visible queda como Tono y el ComboBox muestra solo tonos simples realmente registrados para la voz elegida. Ver `docs/productizacion/DOCUMENT_SIDEBAR_VOICE_UX1_VOZ_IA_AUDIO_LOCAL.md`.

## Estado implementado — VOICE-LIBRARY-SYNC1

Las muestras de referencia se guardan en biblioteca de app/runtime y Documento las consume como voces/tonos disponibles. Esto mantiene la separación: Vista Voces administra referencias reutilizables; Documento asigna voz IA o audio del computador a fragmentos concretos.


Nota de contrato para tests: las muestras son referencias para Coqui/XTTS y no son clips fijos.

- VOICE-CHUNKS-GPU-HF2 corrige el contrato: Voz local simple y Voz IA avanzada reciben el dispositivo elegido; cada runtime puede usar GPU si lo soporta y la app debe informar cuando cae a CPU.


# Vista Voces — contrato operativo, wizard de muestras y sincronización con Documento

Este documento registra el ajuste de alcance posterior a las capturas reales de la Vista Voces y a las decisiones de producto fijadas por el usuario. Tiene prioridad sobre los textos históricos de `VOZ-UX4R`, `VOZ-TTS5A/B` y cualquier roadmap anterior cuando se trabaje el bloque de voces.

## 1. Para qué sirve la Vista Voces

La Vista Voces no es un dashboard decorativo ni un reproductor de clips fijos. Su función es **administrar la biblioteca de voces y muestras de referencia** que luego usa Documento para generar audio por IA.

Debe resolver tres tareas operativas:

1. Ver qué motor de voz está activo y qué alcance tiene.
2. Gestionar voces registradas por el usuario, por ejemplo `María` o `Pepito`.
3. Registrar muestras de referencia por tono/emoción para esas voces.

La asignación de una voz a un fragmento concreto del documento **no ocurre principalmente en Vista Voces**. Ocurre en Documento, en el sidebar izquierdo, donde el usuario elige si un fragmento usa:

- voz generada por IA mediante una voz/tono registrados; o
- un archivo de audio local elegido por el usuario.

## 2. Qué NO debe ser la Vista Voces

- No debe ser una pantalla de métricas o dashboard sin operación.
- No debe mostrar tarjetas decorativas si no habilitan una acción o estado útil.
- No debe mezclar códigos internos como `STY-HAPPY`, `XTTS`, `Coqui` o `Piper` en el flujo común.
- No debe presentar emociones no registradas como si estuvieran disponibles.
- No debe guardar muestras de voz dentro de cada proyecto del usuario.
- No debe tratar las muestras como audios finales para reproducir siempre igual.

## 3. Regla de muestras de referencia

Una muestra como:

- `María · Neutral`
- `María · Enojada`
- `María · Feliz`

no es un clip final que se reproduce tal cual en todos los fragmentos. Es una **muestra de referencia** para que Voz IA avanzada, mediante Coqui/XTTS, pueda sintetizar texto futuro usando esa voz y ese tono.

Ejemplo operativo:

1. En Vista Voces se registra `María` con muestras `Neutral`, `Feliz` y `Triste`.
2. En Documento se selecciona una oración nueva.
3. El usuario elige `María` y `Feliz`.
4. Coqui/XTTS genera esa oración nueva usando la muestra `María · Feliz` como referencia.

## 4. Ubicación de las muestras

Las muestras pertenecen a la biblioteca de voces de la app/runtime/repositorio local, no a cada carpeta de proyecto.

Regla:

- La muestra de voz se guarda en una zona común de biblioteca de voces.
- El proyecto `.docupodcast` guarda referencias a la voz/tono usados, no copia obligatoriamente las muestras dentro del proyecto.
- La integridad debe poder reportar si una voz referenciada ya no existe en la biblioteca local.

Esto permite que una voz registrada se reutilice entre proyectos.

## 5. Captura de voz

La grabación desde micrófono debe hacerse con Java cuando se implemente el wizard. No hace falta Python para grabar audio.

Python/Coqui/XTTS queda para síntesis y clonación/referencia de voz, no para capturar el audio desde micrófono.

El wizard debe apoyarse en una abstracción de aplicación, no en una vista JavaFX que hable directamente con audio de bajo nivel:

- `VoiceSampleRecorderGateway`
- `VoiceSampleRecordingSession`
- `VoiceSamplePlaybackGateway`
- `VoiceSampleStorageService`

La implementación puede usar APIs/librerías Java para capturar WAV.

## 6. ComboBox de tonos/emociones

En todo ComboBox visible al usuario, el valor principal debe ser simple:

```text
Neutral
Feliz
Alegre
Enojado
Triste
Calmado
Serio
Dramático
Sorprendido
```

No usar en ComboBox normal:

```text
STY-HAPPY
Tonos recomendados · Feliz
Catálogo teatral extendido · Dramática
Feliz · configurado
No disponible con voz local simple...
```

La categoría, disponibilidad, motor o explicación deben vivir fuera del ComboBox: como texto auxiliar, tooltip, badge o panel de estado operativo.

## 7. Regla del sidebar izquierdo de Documento

El sidebar de Documento debe funcionar así:

1. El usuario selecciona la voz, por ejemplo `María`.
2. El ComboBox de tono/emoción se llena solo con las muestras registradas para `María`.
3. Si `María` tiene `Neutral`, `Feliz` y `Triste`, solo aparecen esas tres opciones.
4. `Sorprendido` no aparece hasta que `María` tenga una muestra `Sorprendido`.
5. Si el motor activo es Voz local simple, no se prometen tonos expresivos.
6. Si el usuario quiere audio manual, puede elegir un archivo local en vez de usar Voz IA.

Esta regla ya existía parcialmente en Documento y debe conservarse.

## 8. Tanda VOICE-UX-POLISH1A — implementada

### Objetivo

Limpiar la Vista Voces actual antes de agregar grabación. Esta tanda no crea el wizard completo.

### Cambios esperados

- Quitar el botón `Ver estado de voces` si no tiene función real.
- Quitar `Resumen de operación` si solo repite información ya visible en `Configurar motor`.
- Corregir microcopy blanco o de bajo contraste en `Configurar motor`.
- Convertir `Sin prueba generada` en estado textual, no botón ni microtarjeta confusa.
- Hacer que `Abrir configuración completa` use estilo de acción consistente, preferentemente morado/secundario destacado.
- Mostrar voces creadas como lista operativa:
  - `Narrador prediseñado · Voz simple`
  - `María · Voz avanzada`
  - tags solo para emociones realmente registradas.
- No mostrar emociones faltantes como tags.
- No usar códigos internos en la UI común.

### Criterio de salida

La Vista Voces queda sobria y útil: Inicio explica qué hace la vista, Configurar motor permite elegir/probar, Gestionar voces lista lo registrado. Nada decorativo queda si no tiene razón operativa.

## 9. Tanda VOICE-REGISTRATION-WIZARD1

### Objetivo

Crear una subvista real para registrar una voz con muestras de referencia por emoción.

### Flujo esperado

Desde `Gestionar voces`, el botón `Nueva voz` abre una subvista dentro del workspace:

```text
Gestionar voces → Nueva voz
```

Debe existir botón:

```text
← Volver a gestionar voces
```

### Campos mínimos

- Nombre de la voz.
- ComboBox de tono/emoción con labels simples.
- Frase sugerida para actuar según la emoción.
- Estado de grabación.
- Estado de muestra asignada.

### Acciones mínimas

- `Grabar`.
- `Detener`.
- `Reproducir grabación`.
- `Eliminar grabación`.
- `Asignar a este tono`.
- `Guardar voz`.

### Frases sugeridas

El usuario debe recibir una frase que ayude a microactuar.

Ejemplos:

- Neutral: `Esta es una muestra de voz neutral para la narración.`
- Feliz: `Qué buena noticia, me alegra escucharlo.`
- Enojado: `No estoy de acuerdo con esta decisión.`
- Triste: `Lamento mucho que las cosas hayan terminado así.`
- Calmado: `Respiremos un momento y continuemos con tranquilidad.`

Las frases son guía de actuación, no texto obligatorio para todos los proyectos.

### Regla obligatoria

No se puede guardar una voz avanzada sin muestra `Neutral`.

Mensaje humano:

```text
Para registrar una voz avanzada necesitas al menos una muestra Neutral.
```

### Criterio de salida

El usuario puede crear `María`, grabar `María · Neutral`, opcionalmente grabar `María · Feliz` y guardar la voz sin tocar carpetas manualmente.

## 10. Tanda VOICE-LIBRARY-SYNC1

### Objetivo

Hacer que Documento vea inmediatamente las voces y tonos recién registrados.

### Cambios esperados

- Al guardar una voz, se refresca la biblioteca activa.
- Vista Voces actualiza lista y tags.
- Sidebar de Documento actualiza ComboBox de voces.
- Al elegir una voz, Documento actualiza ComboBox de tonos con sus muestras reales.
- Si una muestra se elimina, Documento deja de mostrar ese tono.
- Las asignaciones existentes deben validarse y mostrar aviso humano si una referencia ya no existe.

### Criterio de salida

Después de registrar `María · Neutral/Feliz/Triste`, Documento puede seleccionar `María` y solo ofrece `Neutral`, `Feliz` y `Triste`.

## 11. Tanda DOCUMENT-SIDEBAR-VOICE-UX1

Esta tanda debe ejecutarse después de `VOICE-LIBRARY-SYNC1`, porque el sidebar debe pulirse con el flujo real ya conectado.

### Objetivo

Dejar claro que el fragmento puede usar dos caminos:

1. Voz IA generada desde voz/tono registrados.
2. Audio local elegido por el usuario.

### Cambios esperados

- Labels simples: `Voz`, `Tono`, `Audio`.
- Mensajes sin términos como `fallback`.
- Si falta un tono, decir: `No hay muestra “Enojado”; se usará Neutral.`
- Voz local simple no muestra tonos expresivos.
- Voz IA avanzada solo muestra tonos de la voz seleccionada.

## 12. Tests/guardarraíles mínimos

### VOICE-UX-POLISH1A

- No aparece `Ver estado de voces` sin handler real.
- No aparece `Resumen de operación` si no tiene acción propia.
- ComboBox de tonos no contiene `·`, `STY-`, `Tonos recomendados` ni texto explicativo.
- No hay microcopy blanco ilegible en Configurar motor.
- `Sin prueba generada` no se presenta como botón.

### Estado VOICE-UX-POLISH1A

Implementación aplicada: limpieza operativa de Inicio/Configurar motor/Gestionar voces, sin grabación todavía. Quedan establecidos ComboBox de tonos con nombres simples y filas de voz con tonos realmente registrados.

### VOICE-REGISTRATION-WIZARD1

- Nueva voz abre subvista.
- Existe `Volver a gestionar voces`.
- No permite guardar sin Neutral.
- Grabar/Detener/Reproducir/Eliminar/Asignar existen como acciones reales.
- Las muestras se guardan fuera de la carpeta del proyecto.

### VOICE-LIBRARY-SYNC1

- Documento ve voces nuevas sin reiniciar.
- Documento muestra solo tonos registrados para la voz elegida.
- Al eliminar un tono, desaparece del ComboBox de Documento.
- Voz local simple no muestra emociones.

## 13. Orden de implementación actualizado del bloque de voces

1. `VOICE-UX-POLISH1A`
2. `VOICE-REGISTRATION-WIZARD1`
3. `VOICE-LIBRARY-SYNC1`
4. `DOCUMENT-SIDEBAR-VOICE-UX1`

No implementar grabación antes de limpiar Vista Voces. No pulir Documento antes de tener sincronización real de biblioteca.


## Implementación VOICE-REGISTRATION-WIZARD1

La subvista Nueva voz ya existe como superficie interna de Gestionar voces. Las muestras se tratan como referencias para generar texto nuevo, no como clips fijos. La compuerta de Neutral queda visible antes de terminar una voz avanzada.
