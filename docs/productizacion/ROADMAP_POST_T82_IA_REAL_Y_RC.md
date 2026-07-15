# Roadmap post T82 — IA real, polish y RC

T82 no debe tratarse como release candidate final. Es una tanda de continuidad, documentación y evidencia visual. El programa todavía debe cerrar la promesa central: voz real y transcripción real.

## Secuencia recomendada

### T83 — Preflight real de motores

- Consolidar inspección TTS/STT/FFmpeg.
- Mostrar estado claro en Configuración.
- Guardar resultados de diagnóstico.
- Agregar pruebas con ejecutables fake.

### T84 — Piper TTS real liviano

- Crear perfil oficial Piper.
- Seleccionar ejecutable y voz.
- Generar WAV real por segmento.
- Probar reproducción desde Documento.

### T85 — Whisper.cpp STT real

- Seleccionar ejecutable y modelo.
- Normalizar audio.
- Transcribir audio a texto.
- Guardar transcript y logs.

### T86 — FFmpeg operativo

- Preflight FFmpeg.
- Extraer audio de video.
- Registrar asset original y audio derivado.
- Validar integración con media flexible.

### T87 — UX final de motores

- Configuración guiada.
- Mensajes humanos.
- Pruebas cortas desde UI.
- Errores accionables.

### T88 — Smoke real con motores locales

- DOCX → Piper → reproducir.
- Audio → Whisper → texto.
- Video → audio → asignación a fragmento.
- Guardar/cerrar/reabrir.

### T89 — Packaging de herramientas externas

- Definir qué se embebe y qué se configura.
- Manifiestos de licencias y hashes.
- App-image/MSI si aplica.

### T90 — Release Candidate real

- Solo cuando TTS/STT estén realmente operativos en una máquina Windows de prueba.

## Criterio de no avance a RC

No avanzar a RC si no se cumple:

```text
Escuchar documento genera o reproduce voz real con al menos un motor TTS oficial.
Audio a texto transcribe con al menos una ruta STT oficial.
FFmpeg se verifica o se declara ausente con fallback honesto.
La app puede guardar/reabrir un proyecto con assets de audio/imagen/video-audio.
El usuario normal entiende qué falta cuando no hay motor configurado.
```

