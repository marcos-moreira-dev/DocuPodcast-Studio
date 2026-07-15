# T119 — Configuración honesta y accionable

## Objetivo

Convertir Configuración en una superficie clara y accionable. La pantalla no debe vender promesas falsas: si una acción parece botón, debe ejecutar algo real; si todavía es guía, debe decir explícitamente que es guía informativa.

## Contexto

Antes de esta tanda, Configuración ya era real para guardar preferencias y mostrar preflight, pero aún mezclaba tres niveles:

1. Controles persistentes reales.
2. Botones reales de Coqui/XTTS y Piper agregados en T117/T118.
3. Tarjetas de catálogo que mencionaban “Descargar modelo”, “Importar modelo” o “Probar voz” como texto, lo cual podía parecer una acción disponible aunque no tuviera handler.

Además, la línea de comandos externa aparecía dentro de la sección normal de TTS, demasiado expuesta para un usuario no técnico.

## Cambios de producto

- Configuración sigue siendo el lugar para motores, dispositivos, FFmpeg, almacenamiento y diagnóstico.
- Documento sigue siendo la vista de lectura, sin detalles técnicos dominantes.
- Coqui/XTTS, Piper y FFmpeg tienen acciones reales de verificación en la sección de motores.
- Las tarjetas inferiores de motores quedan como guía informativa, no como botones falsos.
- La línea de comandos externa se mueve a Diagnóstico avanzado.

## Cambios implementados

### 1. Corrección de diagnóstico

Se actualiza `ProjectSourcePortabilityT126ASourceTest` para validar el aviso de fuente portable en `ProjectSourceCopyNoticeDialog`, no directamente en `DocuPodcastShellView`. La extracción del diálogo era correcta; el test había quedado desalineado.

### 2. FFmpeg accionable en Configuración

Se agrega un bloque “Asistente FFmpeg” en Configuración con botón real:

```text
Verificar FFmpeg
```

Este botón usa:

```text
FfmpegRuntimeProbeUseCase
EmbeddedFfmpegLocator
FfmpegRuntimeReport
```

La verificación consulta:

- `ffmpeg -version`
- `ffprobe -version`
- `ffmpeg -hide_banner -encoders`

Y reporta encoders conocidos:

- `libx264`
- `h264_nvenc`
- `h264_qsv`
- `h264_amf`

### 3. Catálogo marcado como guía

`EngineSetupCard` ya no muestra:

```text
Preparación sugerida:
```

Ahora muestra:

```text
Guía informativa, no botón:
```

Así se evita que el usuario crea que “Descargar modelo” o “Importar modelo” ya son botones implementados si todavía forman parte del catálogo de próximos pasos.

### 4. Línea de comandos externa como opción avanzada

La sección normal “TTS y voces” ya no muestra “Comando externo” como control principal. Ahora aparece como:

```text
Comando externo avanzado
```

en Diagnóstico y rendimiento.

El mensaje explícito queda:

```text
La línea de comandos externa queda reservada para Diagnóstico avanzado; el usuario normal debe preparar Coqui/XTTS o Piper desde los asistentes reales.
```

### 5. Lenguaje de video

La sección “Storyboard y video simple” se renombra hacia:

```text
Visuales y video simple
```

para mantener la regla de producto: Storyboard no es módulo principal, los visuales pertenecen al Documento.

## Guardarraíles

Se agrega:

```text
HonestActionableSettingsT119SourceTest
```

Este test protege que:

- existan botones reales de Coqui/XTTS;
- existan botones reales de Piper;
- exista botón real de FFmpeg;
- la línea de comandos externa quede en diagnóstico avanzado;
- el catálogo de motores se marque como guía informativa;
- no vuelva “Preparación sugerida” como si fuera acción real.

## Criterio de aceptación

- Configuración guarda preferencias reales.
- Configuración verifica motores reales desde botones reales.
- Las tarjetas informativas no simulan acciones.
- El usuario normal no ve comando externo como ruta principal.
- No se reintroduce Whisper/STT ni Guion.
