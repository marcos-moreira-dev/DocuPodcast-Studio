# 18 — Exportaciones, formatos y paquetes

Exportar según naturaleza del artefacto.

## Formatos

```text
Guion → Markdown / PDF futuro / DOCX futuro
Audio → WAV / MP3
Storyboard → paquete / PNG preview / video simple futuro
Proyecto → .docupodcast.json + assets
Observabilidad → reporte diagnóstico / logs
```

## Exportación activa

Inspirado en DMS:

```text
ActiveArtifactOutputResolver
ActiveArtifactOutputContributor
ExportableArtifactOutput
ProjectExportFormatPolicy
```

No mostrar formatos si no hay cadena real.

## Paquete de proyecto

```text
export/
  input/      # DOCX fuente, Markdown fuente, PDF fuente
  editable/   # .docupodcast.json + assets
  output/     # audio, guion, storyboard preview
  jobs/       # jobs reanudables o snapshot
  reports/    # README/MANIFEST/reporte
  logs/
```

Si el proyecto nació desde Word, el DOCX debe viajar como `input/`.

## MP3

Solo disponible si existe encoder/FFmpeg configurado.

## Storyboard video

Futuro. No mostrar si no está implementado.
