# T84 — Preflight de motores IA: Coqui obligatorio, Piper fallback, Whisper y FFmpeg

## Resumen ejecutivo

Esta tanda convierte la conversación sobre motores reales en contrato ejecutable. DocuPodcast Studio no debe quedarse eternamente en mock: debe poder guiar al usuario para instalar/configurar pocos motores concretos y probarlos.

La ruta queda cerrada:

| Necesidad | Motor oficial | Rol |
|---|---|---|
| Voz realista / calidad alta | Coqui XTTS | Obligatorio para producto objetivo |
| Voz promedio / demo rápida | Piper | Fallback liviano |
| Audio a texto | whisper.cpp | STT local |
| Video/audio utilitario | FFmpeg | Media, extracción y render simple |

## Decisión fuerte: Coqui no es opcional

Coqui XTTS es obligatorio para la versión que busca calidad de sonido. Piper se mantiene porque permite una demo más sencilla y equipos modestos, pero no sustituye la promesa de calidad.

Por eso `InspectAiEnginesPreflightUseCase` siempre reporta una fila para `tts-xtts-coqui` como `mandatoryForTargetProduct = true`.


## Nota de licencia y distribución

Coqui/XTTS aporta la ruta de calidad alta, pero su modelo y sus outputs tienen condiciones de licencia que deben revisarse antes de distribuirlo o usarlo comercialmente. Por eso T84 no lo incrusta automáticamente en el instalador base: primero se valida carpeta local, wrapper y prueba; luego una tanda posterior debe decidir empaquetado, descarga asistida o importación manual con aceptación explícita de licencia.

## Política de descarga/empaquetado

Los modelos de IA no deben guardarse dentro del `.docupodcast` ni mezclarse con documentos del usuario.

Opciones permitidas para V1:

1. **Importación manual guiada**: el usuario selecciona una carpeta local con el modelo ya descargado.
2. **Descarga asistida por catálogo verificable**: futura tanda con manifest versionado, checksums y mensajes claros.
3. **Distribución separada de modelos**: paquete externo opcional para no inflar el instalador base.


Regla literal para guardarraíles: no se incrustan modelos pesados dentro del .docupodcast; se referencian carpetas locales verificables.

No se permite para V1:

- esconder modelos pesados dentro del proyecto;
- depender de un enlace único hardcodeado;
- mostrar comandos crudos en Documento;
- prometer Coqui listo si solo existe Piper.

## Carpetas recomendadas

```text
models/tts/xtts/
models/tts/piper/voices/
models/stt/whisper/
tools/ffmpeg/bin/ffmpeg.exe
tools/whisper-cpp/whisper-cli.exe
tools/piper/piper.exe
tools/xtts-wrapper/
```

La app debe poder abrir esas carpetas desde Configuración o Diagnóstico, pero Documento debe seguir limpio.

## Contrato de preflight

`AiEnginePreflightReport` responde preguntas de producto:

- ¿Coqui está listo?
- ¿Piper está listo para una demo rápida?
- ¿Whisper puede transcribir?
- ¿FFmpeg puede extraer audio de video?
- ¿La demo mínima de voz está lista?
- ¿La demo completa está lista?

Estados:

```text
READY
READY_WITH_WARNINGS
NEEDS_CONFIGURATION
MISSING_RUNTIME
MISSING_MODEL
OPTIONAL_NOT_CONFIGURED
```

## Qué debe mostrar la futura UI de Configuración

En Configuración, no en Documento:

```text
Motores IA

Coqui XTTS — Calidad alta
Estado: Falta wrapper / Falta modelo / Listo
[Seleccionar carpeta de modelo]
[Seleccionar wrapper]
[Probar voz]

Piper — Motor liviano
Estado: Opcional / Listo
[Seleccionar voz]
[Probar lectura]

whisper.cpp — Audio a texto
Estado: Falta ejecutable / Falta modelo / Listo
[Seleccionar ejecutable]
[Seleccionar modelo]
[Probar transcripción]

FFmpeg — Media
Estado: Falta ejecutable / Listo
[Seleccionar ffmpeg.exe]
[Probar extracción]
```

## Cómo debe funcionar para el usuario normal

Documento solo debe decir:

```text
Falta configurar un motor de voz en Configuración.
```

o:

```text
Generando voz…
Audio listo.
```

No debe decir CUDA, Python, venv, checkpoint, ggml, wrapper ni ffmpeg salvo que el usuario abra Configuración/Diagnóstico.

## Roadmap inmediato

1. `T85 — Piper real end-to-end`: primera voz real y WAV reproducible.
2. `T86 — Coqui XTTS wrapper real`: calidad alta obligatoria.
3. `T87 — whisper.cpp real`: transcripción local.
4. `T88 — FFmpeg real`: extraer audio desde video y pruebas de media.
5. `T89 — UX de motores`: pantalla de configuración clara.
6. `T90 — Smoke real + RC`: demo completa verificable.
