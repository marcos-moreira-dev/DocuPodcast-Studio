# RUNTIME-PATHS-RF1 + EXTERNAL-PROCESS-RUNNER-RF1 + EXTERNAL-PROCESS-EXCEPTIONS-HF1

## Propósito

Esta tanda inicia el cierre de arquitectura transversal para motores locales, smoke CUDA, scripts, FFmpeg, Piper, XTTS y packaging. No agrega nuevas promesas visibles de producto; consolida contratos internos para que lo que ya existe deje de depender de rutas y procesos dispersos.

## Decisiones implementadas

### 1. Rutas runtime centralizadas

Se agrega `application.runtime.RuntimeArtifactPaths` como política explícita de rutas para:

- raíz de aplicación;
- `tools/`;
- `models/`;
- `runtime/`;
- wrapper XTTS;
- Python local de XTTS;
- script Python `synthesize_xtts.py`;
- script PowerShell `scripts/tts/xtts-file-to-wav.ps1`;
- modelo local `models/tts/xtts`;
- smoke de XTTS `runtime/tts/xtts-smoke`;
- manifiesto de readiness WAV;
- manifiesto de CUDA smoke;
- Piper;
- FFmpeg/FFprobe;
- biblioteca de voces de la app;
- muestras de voz;
- temporales de grabación.

La regla es: si un flujo de motor, diagnóstico o packaging necesita una ruta de runtime, debe consultar esta política o una política superior basada en ella. No debe inventar strings nuevos.

### 2. Proceso externo común

Se agrega el contrato:

- `ExternalProcessRequest`;
- `ExternalProcessResult`;
- `ExternalProcessRunner`;
- `infrastructure.process.DefaultExternalProcessRunner`.

El contrato captura:

- comando;
- carpeta de trabajo;
- timeout;
- entorno;
- etiqueta de auditoría;
- stdout;
- stderr;
- código de salida;
- timeout;
- cancelación futura;
- duración;
- cola de salida para diagnósticos.

### 3. Integración focal real

El smoke CUDA de XTTS puede usar el runner común mediante:

```text
ProcessXttsCudaRuntimeProbeGateway(ExternalProcessRunner runner)
```

La fábrica principal inyecta:

```text
new ProcessXttsCudaRuntimeProbeGateway(new DefaultExternalProcessRunner())
```

Así el camino productivo de Configuración usa el contrato común para la prueba CUDA del Python local.

### 4. Excepciones de proceso externo

Se amplía la familia de errores de proceso externo:

- `ExternalProcessFailedException` deja de ser final para admitir especializaciones;
- `ExternalProcessTimeoutException`;
- `ExternalProcessCancelledException`.

Estas excepciones conservan:

- código de salida;
- comando auditado;
- última salida;
- ruta de log;
- mensaje humano;
- detalle técnico.

## Reglas de arquitectura

1. `presentation` no debe construir procesos externos.
2. `application` define contratos y políticas.
3. `infrastructure` ejecuta procesos con `ProcessBuilder`.
4. Los flujos legacy que todavía usen `ProcessBuilder` deben migrarse progresivamente.
5. Ninguna tanda futura debe agregar un `ProcessBuilder` nuevo fuera del runner sin documentar excepción.
6. Ninguna ruta nueva de XTTS/Piper/FFmpeg/voice-library debe hardcodearse fuera de las políticas de runtime.

## Pendiente de migración progresiva

Aún deben migrarse al runner común:

- generación TTS real;
- prueba de voz real;
- FFmpeg runtime probe;
- exportación final de video;
- compresión audio MP3/AAC;
- extracción/normalización de audio con FFmpeg;
- preparación Python portable;
- probes de motores reales.

Esa migración pertenece a `RUNTIME-ARCH-RC1` o tandas específicas de motor.

## Tests agregados

- `RuntimePathsRf1SourceTest`;
- `RuntimeArtifactPathsTest`;
- `ExternalProcessRunnerRf1SourceTest`;
- `ExternalProcessRequestTest`;
- `ExternalProcessExceptionsHf1SourceTest`.

## Criterio de aceptación

La tanda se considera aceptada si:

- los tests pasan;
- el smoke CUDA sigue funcionando;
- no se rompe diagnóstico completo;
- `RuntimeArtifactPaths` existe y se usa en smoke XTTS/CUDA;
- el runner común existe en infraestructura;
- `ApplicationServicesFactory` inyecta el runner en el smoke CUDA;
- las excepciones tipadas de proceso externo existen.
