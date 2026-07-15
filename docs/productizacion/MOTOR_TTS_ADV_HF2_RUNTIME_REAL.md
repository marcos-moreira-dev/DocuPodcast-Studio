# MOTOR-TTS-ADV-HF2 — runtime real de Voz IA avanzada sin contaminación de modo

Estado: implementada sobre VIDEO-EXPORT1-HF verde.

## Motivo

El job real de Voz IA avanzada mostraba `0/1` sin avance visible. El ZIP de `jobs` confirmó que el proceso sí entraba al wrapper Python y llegaba a `cargando_modelo`, pero el motor quedaba presentado como `Voz local simple` y pasaba `cuda:0` aunque el Python local no tenía CUDA disponible. El usuario veía GPU en 0% y la app parecía congelada.

## Cambios

- Los modos gestionados de voz derivan comandos desde la carpeta actual de la tanda/app.
- Una plantilla cruda persistida de una tanda anterior ya no contamina `Voz IA avanzada` ni `Voz local simple`.
- `SettingsAwareAudioGenerationGateway` etiqueta el motor por comando efectivo: wrapper avanzado => `Voz IA avanzada`, Piper => `Voz local simple`.
- La generación avanzada usa CPU por defecto hasta que exista una prueba explícita que confirme CUDA real.
- El gateway publica fases del proceso Python en el estado del job para que la UI no parezca detenida:
  - iniciando proceso local;
  - texto cargado;
  - cargando librerías;
  - GPU no disponible en Python, usando CPU;
  - cargando modelo local;
  - sintetizando;
  - WAV generado.
- El log `generation-log.jsonl` agrega eventos `tts_process_phase`.

## Fuera de alcance

- No se implementa todavía un smoke real de CUDA.
- No se fuerza uso de GPU si Torch local no la detecta.
- No se toca video, exportación ni playback.
