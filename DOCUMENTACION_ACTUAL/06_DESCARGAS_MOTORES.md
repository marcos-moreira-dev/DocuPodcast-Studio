# Descargas de motores y URLs configurables

## Objetivo

Permitir que las rutas de descarga cambien sin tocar código. Esto protege a la app si un proveedor mueve archivos, si se usa mirror interno o si el usuario quiere probar una fuente controlada.

## UX visible

En Configuración > Motores y dependencias se muestran campos editables de URL para:

- Voz IA avanzada: página oficial del modelo XTTS-v2; la app construye internamente URLs por archivo y no muestra `/resolve/main` como página navegable.
- Voz local simple: URL del runtime local.
- Voz local simple: URL de voz neutral.
- Voz local simple: URL de datos de voz neutral.

No se muestran nombres técnicos de motor en textos visibles normales.

## Persistencia

Se guarda en `operational-settings.properties`:

```properties
download.xtts.baseUrl=
download.piper.runtimeZipUrl=
download.piper.defaultVoiceUrl=
download.piper.defaultVoiceMetadataUrl=
```

## Overrides técnicos

Se mantienen para despliegue/soporte por variables de entorno o system properties. No se muestran como texto visible de usuario.

## Criterio de “listo”

Una voz avanzada no queda lista solo por descargar recursos. Estados separados:

1. Descargando.
2. Descargado.
3. Verificado.
4. Prueba WAV generada.
5. Reproducción confirmada dentro de la app antes de documentos largos.
5. WAV reproducido dentro de la app.

## Video local / FFmpeg

La URL de descarga de video local queda centralizada igual que los recursos de voz. La propiedad persistente es:

```properties
download.ffmpeg.runtimeZipUrl=https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip
```

También puede sobrescribirse con:

```text
DOCUPODCAST_FFMPEG_DOWNLOAD_URL
```

O con system property Java:

```text
docupodcast.ffmpegDownloadUrl
```

Desde **FFMPEG-PREP1**, el botón **Preparar** de Video local descarga el ZIP configurado, lo extrae dentro del programa, copia los componentes necesarios a `tools/ffmpeg/bin` y verifica que el runtime sirva para exportación final. **Importar carpeta** queda como alternativa manual/soporte.

## URL de video local / FFmpeg

El componente de video local queda centralizado igual que los motores de voz. La URL predeterminada configurada en código es:

```properties
download.ffmpeg.runtimeZipUrl=https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip
```

Puede editarse desde Configuración o sobrescribirse con:

```text
DOCUPODCAST_FFMPEG_DOWNLOAD_URL
```

También puede configurarse como system property:

```text
docupodcast.ffmpegDownloadUrl
```

Criterio de producto: el usuario no debe preparar FFmpeg manualmente como tarea normal. La exportación de video debe preparar/descargar el componente si falta, verificarlo y luego continuar con el asistente de exportación.

## Nota VOZ-UX4R-1

La descarga/verificación de Voz IA avanzada no se toca en esta tanda. Queda explícitamente pospuesta para **MOTOR-SMOKE4R**, porque primero se está estabilizando la experiencia de Vista Voces. El flujo objetivo debe distinguir: descargado, verificado, seleccionable/usable y prueba generada.

## Alineación VOZ-UX4R-DOC1 — motor y dispositivo desde Vista Voces

La Vista Voces tendrá un módulo **Configurar motor**. Este módulo no duplica otra configuración técnica: escribe y lee el mismo estado interno que Configuración.

Reglas:

- El usuario puede elegir **Voz IA avanzada**, **Voz local simple** o **Modo de prueba** desde Voces.
- La UI normal debe priorizar nombres humanos; `Coqui/XTTS` y `Piper` pueden aparecer solo como detalle técnico o soporte.
- El selector de dispositivo aplica a todos los motores.
- El ComboBox de dispositivo debe usar detección real: CPU, GPU NVIDIA, GPU AMD, GPU Intel o modo Automático si aplica.
- No se permiten placeholders de GPU. Si no se detecta GPU compatible, se muestra CPU con explicación humana.
- Si un motor no puede usar la GPU detectada, debe indicarse honestamente.
- Voz IA avanzada debe distinguir: no descargada, descargando, descargada, verificada, usable y prueba generada.
- Voz local simple puede tener prueba directa con textbox, sin tonos ni muestras humanas.

La descarga robusta de Voz IA avanzada sigue pendiente para **MOTOR-SMOKE4R / COQUI-DL1**. El contrato visual y operativo de Voces queda en `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md`.

## VOZ-UX4R-2B — Configurar motor real dentro de Voces

Se completa el módulo Configurar motor de la microaplicación Voces con selector de motor activo, selector de dispositivo de renderizado CPU/GPU detectado, estado honesto y prueba por textbox. Las selecciones se persisten en `OperationalSettings`, la misma configuración interna usada por Configuración. No se resuelve todavía la descarga de Voz IA avanzada/Coqui; queda para `MOTOR-SMOKE4R / COQUI-DL1`.


## Descarga grande de Voz IA avanzada

La descarga de Voz IA avanzada puede ser pesada. `DownloadXttsOfficialModelUseCase` conserva archivos `.download` y usa HTTP `Range` cuando el servidor lo permite, para reanudar descargas parciales en vez de empezar siempre desde cero. Si el servidor no acepta reanudación, solo se reinicia el archivo afectado; los recursos ya descargados se conservan.

## Diagnóstico de descarga de Voz IA avanzada

Cuando la descarga/preparación de Voz IA avanzada falla o queda incompleta, la app debe dejar un reporte técnico junto a los recursos del modelo:

```text
models/tts/xtts/download-diagnostics.txt
```

Ese archivo es deliberadamente técnico y puede incluir URLs de descarga, códigos HTTP, tamaños reportados por servidor y recursos faltantes. La UI normal mantiene lenguaje humano, pero el reporte sirve para depuración y soporte.
