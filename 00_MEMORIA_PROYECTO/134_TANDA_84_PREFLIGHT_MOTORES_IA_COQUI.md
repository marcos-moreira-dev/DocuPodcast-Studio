# Tanda 84 — Preflight operativo de motores IA con Coqui obligatorio

## Propósito

Esta tanda deja preparado el siguiente paso real del producto: que DocuPodcast Studio pueda decirle al usuario, con claridad, si la máquina ya está lista para generar voz real, transcribir audio y extraer audio desde video.

La decisión de producto queda cerrada:

- **Coqui XTTS es obligatorio** para la ruta de calidad alta. No queda como motor opcional decorativo.
- **Piper** queda como motor promedio/liviano para primera demo funcional y respaldo local.
- **whisper.cpp** queda como motor STT local para audio a texto.
- **FFmpeg** queda como herramienta de media para extraer audio de video y sostener video simple.

Esta tanda no descarga ni empaqueta modelos pesados dentro del proyecto `.docupodcast`. Implementa el contrato de preflight para que Configuración/Diagnóstico pueda guiar al usuario sin convertir Documento en cabina técnica.

## Decisión sobre descarga, empaquetado y carpetas

El usuario preguntó si los componentes se descargarán y se pondrán dentro del programa. La respuesta técnica para V1 queda así:

1. **No se incrustan modelos pesados dentro del `.docupodcast`.**
   El proyecto guarda documentos, assets, capas, audio generado, manifiestos y referencias; no debe transformarse en un archivo monolítico con modelos de IA adentro.

2. **Los motores y modelos viven en carpetas locales verificables.**
   Rutas recomendadas:

   ```text
   tools/ffmpeg/bin/ffmpeg.exe
   tools/whisper-cpp/whisper-cli.exe
   tools/piper/piper.exe
   tools/xtts-wrapper/xtts-wrapper.exe o script equivalente

   models/tts/xtts/
   models/tts/piper/voices/
   models/stt/whisper/
   ```

3. **Coqui/XTTS puede llegar por descarga asistida o importación manual.**
   Por peso, dependencias y licencia, no se debe meter automáticamente dentro del instalador base sin decisión explícita. La app debe permitir:
   - seleccionar carpeta ya descargada;
   - verificar archivos esperados;
   - ejecutar una prueba corta;
   - registrar manifiesto/checksum cuando exista.

4. **Piper puede ser el primer camino de demo real.**
   Sirve para probar `Documento → WAV real → reproducción` con menos fricción que Coqui.

5. **Coqui sigue siendo obligatorio para calidad alta.**
   Piper puede demostrar que la cadena funciona, pero el producto objetivo no se cierra sin Coqui/XTTS.


## Nota de licencia y distribución

Coqui/XTTS aporta la ruta de calidad alta, pero su modelo y sus outputs tienen condiciones de licencia que deben revisarse antes de distribuirlo o usarlo comercialmente. Por eso T84 no lo incrusta automáticamente en el instalador base: primero se valida carpeta local, wrapper y prueba; luego una tanda posterior debe decidir empaquetado, descarga asistida o importación manual con aceptación explícita de licencia.

## Qué se implementó

Se agrega el paquete:

```text
application.engines
```

Con estas piezas:

```text
AiEnginePurpose
AiEngineReadinessStatus
AiEnginePreflightItem
AiEnginePreflightReport
InspectAiEnginesPreflightUseCase
```

El caso de uso `InspectAiEnginesPreflightUseCase` evalúa:

- Coqui XTTS como motor obligatorio de alta calidad.
- Piper como motor liviano/promedio y fallback.
- whisper.cpp como STT local.
- FFmpeg como runtime de media/video.

El resultado sirve para Configuración/Diagnóstico, no para el workspace Documento.

## Estados del preflight

Los estados se reducen a lenguaje operativo:

```text
READY
READY_WITH_WARNINGS
NEEDS_CONFIGURATION
MISSING_RUNTIME
MISSING_MODEL
OPTIONAL_NOT_CONFIGURED
```

Ejemplos:

- Coqui sin wrapper configurado → `MISSING_RUNTIME`.
- Coqui con wrapper pero sin modelo → `MISSING_MODEL`.
- Piper sin configurar → `OPTIONAL_NOT_CONFIGURED`.
- Whisper sin ejecutable → `MISSING_RUNTIME`.
- FFmpeg ausente → `MISSING_RUNTIME`.

## Qué valida Coqui/XTTS

Para el modelo local se reutiliza `ModelFolderContract.xttsHighQuality()`:

```text
config.json
*.pth o *.safetensors
vocab.json o vocab.txt
```

Además se exige un wrapper/comando local que reciba texto y entregue WAV. La app Java no debe incrustar Python, CUDA, entornos virtuales ni dependencias pesadas dentro del lector. El wrapper queda como contrato externo verificable.

## Qué valida Piper

Para Piper se espera:

```text
*.onnx
*.onnx.json o config.json
```

Piper no reemplaza a Coqui como calidad alta. Sirve para:

- demo rápida;
- equipos modestos;
- fallback local;
- pruebas de flujo `texto → WAV`.

## Qué valida whisper.cpp

Para whisper.cpp se revisa:

- ejecutable configurado (`whisper-cli.exe`, `main.exe` o equivalente local);
- modelo local seleccionado (`.bin`, `.gguf`, `.pt`, `.safetensors` o carpeta verificable);
- idioma configurado.

Esto prepara la tanda posterior donde una muestra de audio debe transcribirse desde la aplicación.

## Qué valida FFmpeg

Se revisa:

```text
video.ffmpegExecutable
```

o la ruta embebida esperada:

```text
tools/ffmpeg/bin/ffmpeg.exe
```

FFmpeg es necesario para:

- extraer audio de video;
- normalizar audio;
- preparar/renderizar video simple.

## Integración en servicios

`SettingsApplicationServices` ahora expone:

```text
InspectAiEnginesPreflightUseCase inspectAiEnginesPreflight
```

`ApplicationServicesFactory` lo cablea con:

```text
new InspectAiEnginesPreflightUseCase()
```

Esto deja el preflight disponible para Configuración sin añadir controles al lector central.

## Qué no se hizo en esta tanda

No se descargan modelos.
No se empaqueta Coqui.
No se ejecuta Piper real todavía.
No se transcribe audio todavía.
No se cambia Documento para mostrar rutas técnicas.

La tanda instala el contrato que permite implementar esas pruebas con menor riesgo.

## Próximo paso técnico

La siguiente tanda lógica es:

```text
T85 — Piper real end-to-end
```

Objetivo mínimo:

```text
frase corta → Piper → WAV real → reproducción/verificación
```

Después:

```text
T86 — Coqui XTTS wrapper real
T87 — whisper.cpp real
T88 — FFmpeg real para video/audio
T89 — UX humana de motores
T90 — Smoke real y RC
```

## Guardarraíles agregados

```text
InspectAiEnginesPreflightUseCaseTest
EnginePreflightDocumentationSourceTest
```

Estos tests protegen que:

- Coqui siga siendo obligatorio.
- Piper pueda funcionar como fallback sin reemplazar Coqui.
- La demo completa requiera voz, STT y FFmpeg.
- La documentación diga explícitamente que los modelos pesados no se incrustan en `.docupodcast`.
