# VOZ-UX4R — contrato final de Vista Voces por módulos

## Propósito

La Vista Voces debe funcionar como una microaplicación administrativa de voces dentro de DocuPodcast Studio. Su trabajo no es asignar voces a fragmentos ni exhibir tecnología de motores; su trabajo es mantener una biblioteca de voces lista para que Documento pueda usarla.

La experiencia debe ser sobria, presentable y directa. Esta vista está más cerca de una sección de configuración administrativa que de un dashboard web moderno. No se permiten tarjetas futuristas, métricas decorativas, avatares IA, porcentajes inventados ni “trajetitas” llamativas. La UI debe preferir filas limpias, listas sobrias, estados textuales y acciones claras.

## Regla visual sagrada

La Vista Voces debe verse como un gestor administrativo sobrio:

- filas compactas;
- separadores discretos;
- estados humanos;
- botones justos;
- sin decoración innecesaria;
- sin “front-end web” futurista;
- sin paneles gigantes para datos simples.

Ejemplo esperado para Inicio:

```text
Pepito
Lista para Documento · Neutral, Feliz, Triste, Enojado
[Editar] [Eliminar]
```

No se debe convertir en:

```text
Voice AI Score 85% · Neural Emotion Engine · GPU Ready · Avatar futurista
```

## Módulos finales

La Vista Voces debe tener tres módulos principales:

```text
Voces
├─ Inicio
├─ Configurar motor
└─ Gestionar voces
```

No hace falta, por ahora, separar en módulos adicionales como Probar voz, Archivos, Uso en Documento o Diagnóstico. Esas funciones pueden vivir dentro de los tres módulos anteriores o quedar como acciones secundarias.

## Módulo Inicio

Inicio es el resumen administrativo de la biblioteca. Debe listar las voces registradas y su estado.

Debe responder:

- qué voces existen;
- cuáles están completas;
- cuáles están incompletas;
- qué emociones/tonos tiene cada voz;
- cuáles pueden aparecer en Documento.

Cada fila debe mostrar:

```text
Nombre de voz
Estado · emociones registradas
[Editar voz] [Eliminar]
```

Estados recomendados:

- Lista para Documento;
- Incompleta;
- Falta Neutral;
- Solo Neutral;
- Tonos configurados;
- Usada en Documento;
- Motor no disponible.

Inicio no debe editar muestras ni mostrar controles de grabación. Si el usuario pulsa **Editar voz**, se abre **Gestionar voces** con esa voz seleccionada. Si pulsa **Eliminar**, debe aparecer confirmación destructiva.

## Eliminación de voz

Eliminar una voz siempre debe mostrar un message box. El texto debe advertir que también se eliminarán los archivos relacionados con esa voz.

Mensaje esperado:

```text
Eliminar voz "Pepito"

También se eliminarán las muestras de audio asociadas a esta voz y sus emociones registradas.
Si esta voz está asignada a fragmentos del documento, esas asignaciones se limpiarán o quedarán sin voz asignada.

[Cancelar] [Eliminar voz]
```

Reglas:

- no eliminar modelos del programa;
- no eliminar motores;
- no borrar recursos compartidos por otra voz sin verificar;
- avisar si la voz está usada por fragmentos del Documento;
- limpiar o marcar como huérfanas las asignaciones que dependan de esa voz.

## Módulo Configurar motor

Configurar motor permite elegir el motor activo de lectura y el dispositivo de renderizado desde la misma Vista Voces. Esta pantalla puede redundar con Configuración porque opera sobre el mismo estado interno, no sobre otra configuración separada.

### Selector de motor

Debe existir un selector de motor activo:

```text
Motor de voz
[Voz IA avanzada ▼]
```

Opciones visibles:

- Voz IA avanzada;
- Voz local simple;
- Modo de prueba.

Nombres internos aceptados solo como detalle técnico o soporte:

- Voz IA avanzada puede mapear a Coqui/XTTS;
- Voz local simple puede mapear a Piper;
- Modo de prueba puede mapear a mock/test.

La UI normal debe privilegiar los nombres humanos. No debe llenar el workspace con nombres técnicos.

### Voz local simple

Si el usuario selecciona Voz local simple, el workspace debe mostrar una experiencia mínima:

- estado real del motor;
- selector de dispositivo;
- textbox editable para escribir una frase corta;
- botón **Probar voz**;
- botón para reproducir la última prueba si existe.

Debe deshabilitar o explicar que no aplican:

- voces personalizadas;
- emociones;
- muestras humanas;
- clonación;
- tonos expresivos.

Texto recomendado:

```text
La Voz local simple usa lectura neutral. No usa muestras humanas, emociones ni voces por personaje.
```

### Voz IA avanzada

Si el usuario selecciona Voz IA avanzada, el módulo habilita:

- voces personalizadas;
- muestras por emoción;
- grabación/importación;
- prueba generada;
- uso de CPU/GPU cuando el motor lo soporte.

Estados del motor avanzado:

- No descargado;
- Descargando;
- Descargado;
- Verificado;
- Listo para usar;
- Prueba generada;
- Error.

No basta con decir “Disponible”. Debe distinguir descargado, verificado, seleccionable/usable y probado.

### Selector de dispositivo

El selector de dispositivo aplica a todos los motores. El usuario construye esta aplicación porque quiere aprovechar GPU, por lo que el control debe estar visible en Configurar motor.

Debe ser un ComboBox real, basado en detección, no placeholder.

Opciones posibles:

```text
Automático
CPU
GPU NVIDIA detectada
GPU AMD detectada
GPU Intel detectada
```

Si no hay GPU compatible:

```text
CPU (sin GPU compatible detectada)
```

Si se detecta GPU pero un motor no puede usarla, la UI debe decirlo honestamente. No se debe prometer GPU falsa.

## Módulo Gestionar voces

Gestionar voces crea, edita, reemplaza muestras, elimina y exporta voces. Debe tener arriba un selector de voz existente y una acción de nueva voz.

```text
Voz
[Seleccionar voz ▼]  [+ Nueva voz]
```

Al seleccionar una voz, se cargan sus emociones y muestras. Al crear una voz, el usuario solo escribe el nombre base:

```text
Nombre de voz
[Pepito]
```

El sistema no debe pedir “Pepito feliz”, “Pepito triste” o “Pepito enojado” como voces separadas. Una voz es una entidad única y sus emociones son muestras asociadas.

Estructura conceptual:

```text
Pepito
├─ Neutral
├─ Feliz
├─ Triste
└─ Enojado
```

## Emociones y tonos

El producto debe soportar muchas emociones porque DocuPodcast puede usarse de forma artística. No se debe diseñar con solo tres emociones hardcodeadas.

Catálogo inicial recomendado:

- Neutral;
- Feliz;
- Triste;
- Enojado;
- Misterioso;
- Suave;
- Enérgico;
- Tenso;
- Preocupado;
- Esperanzado;
- Melancólico;
- Inspirador;
- Dramático;
- Reflexivo;
- Sereno;
- Temeroso;
- Sorprendido;
- Épico;
- Documental;
- Académico.

Neutral es obligatoria para que una voz aparezca como usable en Documento. Las demás emociones son muestras de referencia: ayudan a orientar la lectura, pero no garantizan una emoción exacta en todos los motores.

## Reemplazar una emoción

En Gestionar voces, el usuario debe poder seleccionar una voz, seleccionar una emoción y reemplazar esa emoción.

Flujo:

```text
Voz: Pepito
Emoción: Triste

[Importar audio]
[Grabar nuevamente]
[Reproducir muestra]
[Eliminar muestra]
```

Si se importa un nuevo audio para Triste, se reemplaza la muestra Triste de Pepito. Si se graba nuevamente, se reemplaza la muestra Triste de Pepito. No se duplica la emoción y no se crea una voz nueva.

## Grabación por emoción

Al grabar, el workspace debe mostrar una frase guía relacionada con la emoción seleccionada. Esto ayuda al actor a interpretar mejor.

Ejemplos de frases guía:

- Neutral: “Hoy vamos a revisar el documento con una lectura clara y natural.”
- Feliz: “Me alegra mucho que hayamos encontrado una buena solución.”
- Triste: “No fue fácil aceptar lo que ocurrió, pero era necesario seguir adelante.”
- Enojado: “Esto no puede volver a pasar; necesitamos una respuesta clara ahora.”
- Misterioso: “Nadie sabía qué había detrás de esa puerta, pero todos podían sentirlo.”
- Suave: “Respira con calma; todo puede resolverse paso a paso.”
- Enérgico: “Vamos, este es el momento de avanzar con decisión.”
- Tenso: “Algo no encaja, y todos en la sala lo saben.”

Flujo esperado:

```text
Emoción: Triste
Frase guía: "No fue fácil aceptar lo que ocurrió, pero era necesario seguir adelante."

[Grabar]
[Detener]
[Reproducir grabación]
[Guardar muestra]
```

También debe existir **Importar audio** como alternativa a grabar.

## Exportar muestras

Gestionar voces debe permitir exportar las muestras de la voz seleccionada a una carpeta elegida por el usuario.

Ejemplo de salida:

```text
Pepito/
├─ neutral.wav
├─ feliz.wav
├─ triste.wav
└─ README.txt
```

Primera versión suficiente: exportar la voz seleccionada. Más adelante puede ampliarse a exportar todas las voces.

## Integración con Documento

El sidebar Audio de Documento debe consumir esta biblioteca real.

Si el origen es Voz local simple:

- no mostrar emociones;
- no mostrar voces personalizadas;
- usar lectura neutral.

Si el origen es Voz IA avanzada:

- el combo Voz solo muestra voces con Neutral registrada;
- una voz con solo Neutral sí aparece;
- el combo Emoción solo muestra emociones registradas para esa voz;
- al cambiar de voz, se recalcula el combo de emociones;
- si una asignación tenía una emoción que ya no existe, se debe usar Neutral y avisar suavemente.

Ejemplo:

```text
Pepito: Neutral, Feliz, Triste
María: Neutral, Enojado
```

Si el usuario elige Pepito, Documento muestra Neutral, Feliz y Triste. Si cambia a María, muestra Neutral y Enojado. No se muestran emociones que no existan para la voz seleccionada.

## Roadmap de implementación alineado

1. **VOZ-UX4R-2A — shell modular simple de Voces**
   - Módulos: Inicio, Configurar motor, Gestionar voces.
   - Sin SplitPane clásico de detalle permanente.
   - Filas sobrias y navegación administrativa.

2. **VOZ-UX4R-2B — Configurar motor real en Voces**
   - Selector de motor.
   - Estado real.
   - Selector CPU/GPU real para todos los motores.
   - Textbox de prueba y botón Probar voz.

3. **VOZ-UX4R-3 — Gestionar voces real**
   - Crear/editar/eliminar voces.
   - Importar/grabar/reemplazar muestras por emoción.
   - Reproducir muestra.
   - Exportar muestras.
   - Confirmación destructiva al eliminar.

4. **VOZ-TTS5 — Documento consume voces/emociones reales**
   - Combo de voces con Neutral.
   - Combo de emociones registradas por voz.
   - Fallback Neutral.
   - Generación de chunks con voz/emoción asignada.

5. **MOTOR-SMOKE4R / COQUI-DL1**
   - Descargar, verificar, seleccionar y probar Voz IA avanzada.
   - Distinguir descargado, verificado, usable y prueba generada.

6. **MOTOR-PERF1 / MOTOR-GPU1**
   - Detección honesta CPU/GPU.
   - Uso real de GPU si el motor lo soporta.
   - Sin placeholders.

## Criterio de aceptación documental

Antes de implementar la siguiente tanda, la documentación debe conservar estas reglas:

- solo tres módulos principales;
- filas sobrias;
- muchas emociones soportadas;
- Neutral obligatoria;
- reemplazo de emoción por importar o grabar;
- eliminación con message box y borrado de archivos asociados;
- selector de dispositivo para todos los motores;
- Documento filtra voces/emociones reales;
- Configurar motor en Voces escribe sobre la misma configuración interna que Configuración.
