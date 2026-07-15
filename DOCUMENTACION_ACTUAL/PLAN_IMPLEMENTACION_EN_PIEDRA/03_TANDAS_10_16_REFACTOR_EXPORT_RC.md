# Tandas 12–18 — refactor transversal, exportaciones y persistencia RC

Este bloque reduce hardcoding y duplicación, y cierra audio/video/persistencia como producto real. La numeración se actualiza tras dividir el bloque de voces en `VOICE-UX-POLISH1A`, `VOICE-REGISTRATION-WIZARD1` y `VOICE-LIBRARY-SYNC1`.

---

## 12. RF-TX2A — Runtime paths + labels de motores

### Hallazgo de lectura

Existe `ApplicationRuntimeLayout`, pero muchas clases repiten rutas como:

- `tools/ffmpeg/bin/ffmpeg.exe`
- `tools/ffmpeg/bin/ffprobe.exe`
- `tools/piper/piper.exe`
- `tools/xtts-wrapper/.venv/Scripts/python.exe`
- `tools/xtts-wrapper/synthesize_xtts.py`
- `models/tts/xtts`
- `models/tts/piper/voices`
- `runtime/tts/xtts-smoke`

También se repiten labels como `Voz IA avanzada`, `Voz local simple` y `Modo de prueba`.

### Objetivo

Una sola verdad para rutas y nombres humanos de motores.

### Alcance técnico

Crear o ampliar:

- `RuntimeArtifactPaths`
- `EngineModeCatalog`

Aplicar a:

- templates XTTS/Piper;
- inspectores de readiness;
- downloaders;
- Settings;
- Document/Voces;
- preflight.

### Criterio de salida

Descarga, verificación y generación usan las mismas rutas. Settings, Voces y Documento usan los mismos labels.

---

## 13. RF-TX2B — Mensajes humanos transversales

### Hallazgo de lectura

`SettingsDialog` contiene una lógica grande de reemplazo de términos técnicos, por ejemplo traducir Coqui/XTTS/Piper/FFmpeg a términos humanos.

### Objetivo

Mover mensajes humanos a una política común.

### Alcance técnico

Crear:

- `HumanEngineMessagePolicy`
- eventualmente `HumanRuntimeMessagePolicy`

Debe cubrir:

- errores de motor;
- descarga;
- preflight;
- GPU/CPU;
- nombres técnicos permitidos solo en soporte avanzado;
- sanitización de rutas/URLs para UI común.

### Criterio de salida

La UI común no muestra nombres técnicos salvo en diagnóstico/soporte avanzado.

---

## 14. RF-TX2C — ExternalProcessRunner

### Hallazgo de lectura

Hay `ProcessBuilder` repartido en múltiples clases: TTS local, pruebas de voz, FFmpeg audio/video, probes, GPU y setup Python.

### Objetivo

Unificar ejecución de procesos externos.

### Crear

- `ExternalProcessRunner`
- `ExternalProcessRequest`
- `ExternalProcessResult`
- `ExternalProcessCancellationPolicy`
- `ExternalProcessLogCollector`

### Reglas

- Timeout explícito.
- Stdout/stderr capturados.
- Cancelación uniforme.
- Kill de procesos hijos.
- Logs guardables.
- Mensajes humanos desde política común.

### Aplicación progresiva

No migrar todo en una tanda enorme. Prioridad:

1. XTTS/Python.
2. FFmpeg video.
3. FFmpeg audio.
4. Piper.
5. GPU smoke/probes.

### Criterio de salida

Los procesos externos críticos se ejecutan con el mismo contrato de timeout/cancelación/logs.

---

## 15. RF-TX2D — ManagedDownloadService

### Hallazgo de lectura

Descargas de XTTS, Piper y FFmpeg usan `HttpClient` y lógica propia en varios casos de uso.

### Objetivo

Unificar descargas.

### Crear

- `ManagedDownloadService`
- `DownloadProgressReporter`
- `DownloadResumePolicy`
- `DownloadFailureHumanizer`

### Reglas

- Progreso consistente.
- Descarga parcial/reanudación.
- Timeout y fallos humanos.
- Validación final.
- Checksum cuando aplique.

### Criterio de salida

Descargar XTTS/Piper/FFmpeg se siente como el mismo sistema, no tres implementaciones distintas.

---

## 16. AUDIO-EXPORT-FINAL-HF1 — exportación de audio en segundo plano

### Hallazgo de lectura

Audio final WAV/MP3/AAC existe, pero puede ejecutarse desde el hilo UI. MP3/AAC usan FFmpeg y pueden tardar.

### Objetivo

Evitar congelamiento al exportar audio.

### Alcance técnico

Revisar y ajustar:

- `ExportPodcastWavUseCase`
- `ExportPodcastAudioUseCase`
- `PcmWavConcatenator`
- `ExportWorkflowCoordinator`
- `DocuPodcastShellView`
- `DocuPodcastShellViewModel`
- `InspectExportReadinessUseCase`

### Cambios esperados

- Exportación con `Task`.
- Progreso simple.
- Cancelación para MP3/AAC.
- Fases visibles:
  - preparando WAV;
  - concatenando segmentos;
  - comprimiendo MP3/AAC;
  - verificando salida;
  - completado/fallido/cancelado.

### Criterio de salida

Exportar un documento largo a WAV/MP3/AAC no congela la app.

---

## 17. VIDEO-RUNTIME-SMOKE1 — smoke real de MP4 final

### Hallazgo de lectura

Exportación MP4 final está bien armada: calidad, FileChooser, `Task`, progreso, cancelación y FFmpeg. Falta smoke real en Windows.

### Objetivo

Validar que FFmpeg produce un MP4 reproducible.

### Alcance técnico

Revisar y ajustar:

- `ExportFinalVideoUseCase`
- `FinalVideoExportRequest`
- `VideoRenderProgressView`
- `FfmpegRuntimeProbeUseCase`
- `EmbeddedFfmpegLocator`
- scripts de smoke/diagnóstico.

### Smoke mínimo

- 1 imagen.
- 1 WAV corto.
- 720p.
- Exportar `.mp4`.
- Verificar que existe y pesa más de 0 bytes.
- Verificar cancelación en un caso controlado.

### Política de temporales

- Éxito: limpiar clips temporales.
- Fallo/cancelado: conservar para diagnóstico.

### Criterio de salida

La app produce MP4 real con FFmpeg local y reporta fallo/cancelación de forma humana.

---

## 18. PERSISTENCE-RC1 — guardar/reabrir proyecto completo

### Hallazgo de lectura

La persistencia ya rehidrata documento, guion interno, storyboard, voces, muestras, capas y jobs. Falta smoke de usuario completo.

### Objetivo

Validar roundtrip real de proyecto.

### Smoke RC

1. Abrir DOCX.
2. Preparar lectura.
3. Asignar voz/tono.
4. Asignar imagen.
5. Generar audio.
6. Exportar audio.
7. Exportar video.
8. Guardar proyecto.
9. Cerrar.
10. Reabrir.
11. Reproducir sin regenerar.
12. Exportar de nuevo.

### Alcance técnico

Revisar:

- `ProjectWorkflowCoordinator`
- `LoadProjectWorkspaceArtifactsUseCase`
- `ProjectWorkspaceHydration`
- `ProjectRoundTripUseCase`
- `ValidateProjectWorkspaceIntegrityUseCase`
- `InspectProjectIntegrityUseCase`
- repositorios de document/script/storyboard/voice/audio jobs.

### Criterio de salida

Un proyecto real conserva documento, voces, tonos, muestras, imágenes, audio, jobs y estado útil después de reabrir.
