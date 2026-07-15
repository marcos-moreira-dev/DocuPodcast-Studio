# T85 — Piper TTS real end-to-end

## Decisión de producto

Piper queda como **primer motor funcional liviano**. Sirve para que el usuario pueda comprobar que DocuPodcast Studio realmente convierte un documento en audio generado por máquina. No es la meta de calidad final: **Coqui XTTS sigue siendo obligatorio** para el motor de voz de alta calidad.

La aplicación no debe descargar ni incrustar modelos grandes de forma silenciosa. El criterio sigue siendo:

- herramientas en `tools/`;
- modelos en `models/`;
- configuración guiada;
- preflight claro;
- Documento sin comandos técnicos.

## Contrato técnico

### Rutas esperadas

```text
scripts/tts/piper-file-to-wav.ps1
tools/piper/piper.exe
models/tts/piper/voices/<voz>.onnx
```

### Configuración mínima

```properties
tts.engineMode=piper
tts.voiceProfileId=<nombre-de-voz-sin-extension-o-ruta-onnx>
```

Si `tts.commandTemplate` está vacío, DocuPodcast deriva la plantilla automáticamente. Si el usuario avanzado define `tts.commandTemplate`, esa plantilla manual tiene prioridad.

### Resultado operativo

El objetivo exacto de T85 es:

```text
Documento → segmentos narrables → Piper → texto → WAV real → manifest → reproducción
```

Esto significa **texto → WAV real** usando Piper, sin pasar por servidor HTTP y sin depender de nube.

## Componentes agregados

### `PiperTtsCommandTemplate`

Responsable de construir el comando real a partir de `OperationalSettings`.

Responsabilidades:

- detectar `engineMode=piper`;
- respetar `tts.commandTemplate` si el usuario avanzado lo configuró;
- resolver la voz `.onnx` desde `models/tts/piper/voices/`;
- usar `scripts/tts/piper-file-to-wav.ps1` como puente text-file → stdin;
- mantener placeholders `{textFile}` y `{outputFile}` para el gateway existente.

### `scripts/tts/piper-file-to-wav.ps1`

Responsable de:

- validar `piper.exe`;
- validar modelo `.onnx`;
- validar archivo de texto;
- leer el texto con UTF-8;
- enviarlo a Piper;
- validar que el WAV fue creado.

## Relación con Coqui

Piper no reemplaza Coqui. Piper permite cerrar una demo útil y rápida. Coqui XTTS queda como ruta de calidad alta y debe venir en T86 como wrapper externo compatible con el mismo contrato de salida WAV.

## Riesgos conocidos

- La voz Piper debe coincidir con idioma y modelo disponible.
- Algunos modelos pueden requerir archivo de metadatos adicional `.json`; el preflight debe advertirlo.
- PowerShell debe estar disponible en Windows.
- Piper debe poder escribir WAV en la ruta de salida del job.
- Si la voz no existe, el job fallará limpiamente y dejará diagnóstico.

## Guardarraíles

Nuevos tests:

- `PiperTtsCommandTemplateTest`
- `PiperRealTtsEndToEndSourceTest`

Estos tests no descargan modelos. Verifican que el contrato de comando, wrapper y documentación existan.
