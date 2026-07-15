# MOTOR-SMOKE4R-HF1 — URL real de Coqui/XTTS y normalización de descarga

## Motivo

Tras MOTOR-SMOKE4R / COQUI-DL1, la preparación de Voz IA avanzada podía inducir a error porque la configuración heredada podía mostrar o conservar `https://huggingface.co/coqui/XTTS-v2/resolve/main/` como si fuera una página web navegable. Ese endpoint sin nombre de archivo puede verse como 404 al abrirlo en navegador.

Además, el usuario recordó que el flujo que le funcionaba usaba el proyecto oficial `https://github.com/coqui-ai/TTS`. Ese repositorio es la fuente del paquete/librería Coqui TTS; los artefactos del modelo XTTS-v2 se resuelven como archivos concretos desde el repositorio oficial del modelo.

## Cambios

- `OperationalSettings.TtsEngineSettings.DEFAULT_XTTS_DOWNLOAD_BASE_URL` ahora apunta a la página navegable `https://huggingface.co/coqui/XTTS-v2`, no al endpoint raw `/resolve/main/`.
- `DownloadXttsOfficialModelUseCase` normaliza entradas heredadas:
  - página del modelo;
  - `/tree/main`;
  - `/blob/main`;
  - `/resolve/main`;
  - URLs con archivo concreto como `config.json?download=1`.
- Si se pega `https://github.com/coqui-ai/TTS`, la app lo reconoce como repositorio de código Coqui TTS y usa la página oficial del modelo XTTS-v2 para descargar artefactos de modelo, evitando construir URLs inexistentes de GitHub con `/resolve/main/`.
- `SettingsDialog` normaliza el campo al cargar/guardar. En HF2 la etiqueta visible pasa a `Página oficial del modelo de voz` para no exponer nombres técnicos en presentación.
- Se agregan guardarraíles:
  - `DownloadXttsOfficialModelUrlNormalizationTest`.
  - `AdvancedVoiceDownloadUrlHf1SourceTest`.

## Regla de producto

La UI puede mostrar una página humana y navegable. La descarga técnica debe construir internamente URLs por archivo. Nunca se debe presentar `/resolve/main` como si fuera una página para abrir en navegador.

## Fuera de alcance

- No se cambia playback.
- No se cambia Documento.
- No se cambia Vista Voces.
- No se resuelve todavía la reproducción confirmada del WAV de prueba.
- No se implementa GPU real.
