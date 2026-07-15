# Configuración operativa T67

## Contrato

La configuración operativa pertenece al cerebro de DocuPodcast Studio. No forma parte del documento fuente y no debe ensuciar la experiencia de lectura.

## Familias de ajustes

1. Lectura/documento: tamaño de letra, interlineado y seguimiento visual.
2. Reproducción/buffer: fragmentos iniciales, lookahead y pausa si falta audio.
3. TTS/voz: motor, comando externo, idioma, voz, timeout y reintentos.
4. STT: ejecutable Whisper, modelo, idioma y timeout.
5. Storyboard/video: FFmpeg, resolución y preferencia por binario embebido.
6. Almacenamiento: carpetas de modelos y exportaciones.
7. Diagnóstico: preflight, logs y manifests.

## Persistencia

La implementación base usa `PropertiesOperationalSettingsRepository` y guarda en:

```text
${user.home}/.docupodcast-studio/operational-settings.properties
```

Esta ruta podrá evolucionar, pero el principio queda fijo: las preferencias viven fuera del Word/PDF/Markdown/TXT.

## Integración real

La configuración ya no es solo texto de ayuda. `InfrastructureServicesFactory` carga `OperationalSettings` y con esos valores arma la configuración de TTS y STT.

## Límite de esta tanda

T67 no implementa todavía controles editables completos en JavaFX. Deja el contrato, persistencia, validación y lectura de valores operativos listos para que T68/T69 usen esa base sin rehacer el cerebro.
