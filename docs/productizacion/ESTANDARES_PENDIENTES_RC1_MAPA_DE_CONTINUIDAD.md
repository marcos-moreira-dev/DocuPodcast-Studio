# ESTÁNDARES-PENDIENTES-RC1 — mapa de continuidad exhaustivo

## Resumen ejecutivo

El producto está estable para demo y diagnóstico base, pero aún quedan estándares de cierre antes de una RC personal. Este documento no propone nuevas funciones de usuario; documenta estándares pendientes para que el cierre sea robusto, mantenible y verificable.

## Última base verde conocida

```text
Diagnóstico completo OK.
Demo teatral estable.
Piper y FFmpeg locales OK.
Smoke automático cerebro OK.
```

## Warning CSS observado y corrección

Warning:

```text
Could not resolve '-dp-text-secondary'
from rule '*.example-project-readiness'
```

Corrección:

```css
-dp-text-secondary: -docu-text-muted;
```

Este token es un alias de compatibilidad para superficies de ejemplos. No debe eliminarse mientras `examples.css` lo use.

## Estándar 1: UI operativa

Toda UI debe responder a:

```text
qué hago aquí;
qué decisión tomo;
qué estado necesito saber;
qué riesgo se evita;
cuál es el siguiente paso.
```

Elementos prohibidos:

```text
cards decorativas;
resúmenes sin acción;
indicadores sin consecuencia;
texto técnico sin decisión;
botones sin handler;
placeholders visibles.
```

## Estándar 2: decisiones visibles

Fallback defensivo visible:

```text
GPU→CPU
Voz IA avanzada→motor alternativo
Tono pedido→Neutral
Exportación solicitada→bloqueada o parcial
Proyecto íntegro→abierto con advertencias
```

## Estándar 3: runtime centralizado

Rutas deben concentrarse en `RuntimeArtifactPaths`.

No duplicar:

```text
tools/xtts-wrapper
models/tts/xtts
runtime/tts/xtts-smoke
tools/piper
tools/ffmpeg
voice-library/samples
```

## Estándar 4: procesos externos

Toda ejecución externa debe pasar por un runner común:

```text
ExternalProcessRunner
```

Debe capturar:

```text
exitCode;
timeout;
cancelación;
stdout/stderr tail;
command audit;
diagnostic path.
```

## Estándar 5: motores honestos

Superficies:

```text
Documento: solo usable.
Configuración: todo con estado.
Voces: motor activo y capacidad real.
Diagnóstico: detalle técnico.
```

No decir listo sin prueba real.

## Estándar 6: persistencia

Roundtrip debe conservar:

```text
documento;
capas;
anclas;
imágenes;
audio;
voces;
jobs;
readiness;
demo visual bindings.
```

## Estándar 7: comandos

Cada comando visible debe tener:

```text
handler;
availability;
razón si deshabilitado;
superficie permitida;
sin placeholder.
```

## Tandas restantes documentadas

Ver:

```text
DOCUMENTACION_ACTUAL/08_ESTANDARES_PENDIENTES_RC.md
DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md
```
