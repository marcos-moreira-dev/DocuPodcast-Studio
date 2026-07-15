# Roadmap post T84 — Cerrar IA real sin dispersar motores

## Estado tras T84

El código ya tiene preflight de motores IA/media y una decisión cerrada de shortlist. Todavía no genera voz real desde Piper ni Coqui en esta tanda.

## Próximas tandas

### T85 — Piper real end-to-end

Objetivo: demostrar que DocuPodcast puede generar un WAV real desde texto.

Criterio de aceptación:

- Configurar `piper.exe` y voz `.onnx`.
- Generar una frase corta.
- Guardar WAV en carpeta del proyecto.
- Reproducir o al menos verificar WAV válido.

### T86 — Coqui XTTS wrapper real

Objetivo: cerrar calidad alta.

Criterio de aceptación:

- Configurar wrapper XTTS/Coqui.
- Verificar carpeta `models/tts/xtts`.
- Probar una frase con voz de referencia autorizada.
- Registrar logs y errores humanos.

### T87 — whisper.cpp real

Objetivo: transcribir audio local.

Criterio de aceptación:

- Configurar `whisper-cli.exe`.
- Configurar modelo local.
- Transcribir WAV corto.
- Guardar TXT/manifest.

### T88 — FFmpeg real para media

Objetivo: extracción de audio desde video.

Criterio de aceptación:

- Seleccionar MP4/MOV/MKV/WEBM.
- Extraer WAV.
- Registrar asset original y derivado.

### T89 — UX de motores

Objetivo: Configuración usable.

- Mostrar tarjetas de estado.
- Botones de seleccionar carpeta/ejecutable/modelo.
- Mensajes humanos.
- No exponer comandos crudos en Documento.

### T90 — RC real

Objetivo: cerrar demo completa.

- DOCX → audio real.
- Audio → texto.
- Video → audio extraído.
- Guardar/reabrir.
- Exportar paquete con manifests.
