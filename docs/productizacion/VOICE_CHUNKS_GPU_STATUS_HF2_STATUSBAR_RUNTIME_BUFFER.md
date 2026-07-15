# VOICE-CHUNKS-HF2 — barra de estado legible, runtime XTTS fijado y continuidad de buffer

## Motivo

Durante la prueba visual posterior a `DOCUMENT-SIDEBAR-VOICE-UX1` se reportaron cuatro problemas operativos:

1. Voz IA avanzada fallaba al generar WAV por una incompatibilidad de `transformers`: `BeamSearchScorer` no estaba disponible en la versión instalada dentro del Python autocontenido.
2. La barra de estado mostraba botones largos y comprimidos.
3. Al quedarse sin chunks, la reproducción podía no reanudarse automáticamente al recuperarse el buffer.
4. Al seleccionar un título principal, generar desde ese punto y reproducir, el flujo podía saltar al subtítulo o al primer cue disponible cercano en vez de esperar el cue exacto del título.

## Decisión

Esta tanda es un hotfix funcional antes de continuar con las tandas de cierre.

- El runtime de Voz IA avanzada fija `transformers==4.44.2` junto con `TTS==0.22.0`.
- `check_xtts_runtime.py` verifica explícitamente `BeamSearchScorer` para detectar el runtime roto antes de intentar generar documentos.
- El wrapper `synthesize_xtts.py` devuelve un mensaje reparable si las dependencias Python están incompatibles.
- La barra de estado usa etiquetas cortas: `Renderizar desde aquí`, `Rehacer chunks`, `Seguir generando` y `Detalles`.
- Los botones de la barra tienen ancho mínimo propio y estilo `pressed/armed` claro, sin fondo oscuro que oculte el texto.
- El arranque de playback desde selección no cae al primer cue disponible si el cue exacto del título/fragmento seleccionado todavía no existe.
- Si un job de continuación genera un manifest nuevo que empieza después del cue que causó el hueco de buffer, la reproducción toma el primer cue disponible del nuevo manifest y continúa sin requerir otro clic manual.

## Validación esperada

- Reejecutar `scripts\\20-preparar-python-portable-coqui.bat` o la acción Preparar Voz IA avanzada debe reparar el entorno Python local y bajar la versión compatible de `transformers`.
- La barra de estado no debe mostrar textos cortados en una ventana normal.
- Pulsar un botón de la barra de estado no debe dejar texto oscuro sobre fondo oscuro.
- Si el usuario pide reproducir desde un título, el sistema debe esperar el chunk del título y no saltar al subtítulo por tener otro cue disponible primero.
- Si la lectura se queda esperando buffer, debe reanudarse cuando haya cue recuperable sin otro clic manual.
