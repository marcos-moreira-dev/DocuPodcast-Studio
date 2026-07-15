# Smoke exploratorio mínimo — Tanda 58C

## Propósito

Ejecutar una validación manual corta sobre la base T58B ya verde. El objetivo no es aprobar un release candidate, sino descubrir si el flujo mínimo de usuario funciona antes de cambiar criterios de diseño, scaffolding o arquitectura.

## Preparación

Desde Windows:

```bat
scripts-ejecutar-tests.bat
```

Resultado requerido antes de iniciar:

```text
Tests OK
0 failures
0 errors
```

Documento sugerido:

```text
samples\smoke\documento-simple-t58c.docx
```

## Flujo mínimo

### 1. Inicio y proyecto

- Abrir la app con `scripts-ejecutar-app.bat`.
- Crear un proyecto nuevo.
- Confirmar que la pantalla inicial no obliga a entrar en Guion, Audio, Storyboard o Configuración.

### 2. Importación DOCX

- Importar `samples\smoke\documento-simple-t58c.docx`.
- Confirmar que se renderiza como documento legible.
- Confirmar que los diagnósticos no bloquean el flujo principal.

### 3. Escuchar documento

- Pulsar la acción principal de escuchar/reproducir documento.
- Confirmar que se crea guion si falta.
- Confirmar que pide guardar si hace falta para generar audio.
- Confirmar que se genera audio mock/local por fragmentos.
- Confirmar que inicia reproducción cuando hay buffer suficiente.

### 4. Playback visual

- Confirmar que se puede pausar/reanudar.
- Confirmar que el bloque/oración activa se resalta.
- Confirmar que el scroll no obliga al usuario a perseguir manualmente el texto.

### 5. Guardar y reabrir

- Guardar proyecto.
- Cerrar app.
- Reabrir app.
- Abrir el proyecto guardado.
- Confirmar que documento, guion y estado básico de audio/manifest siguen disponibles.

## Clasificación de hallazgos

Usar estas etiquetas en el reporte:

```text
FUNCIONAL: no se pudo completar un paso básico.
UX: se pudo completar, pero fue confuso o demasiado técnico.
ARQUITECTURA: funciona, pero se observa fragilidad de estado, dirty innecesario, salto de workspace o acoplamiento visible.
DOCUMENTACION: el comportamiento no coincide con README, ayuda, mensajes o checklist.
```

## Criterio de salida

La tanda se considera útil aunque el smoke falle, siempre que el reporte deje claro qué bloquea el flujo mínimo.

No pasar a refactor grande sin decidir primero si los hallazgos son funcionales o solo visuales.
