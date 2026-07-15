# Exportaciones

DocuPodcast debe exportar según el artefacto real, no según la pantalla.

## Salidas

| Artefacto | Exportaciones |
|---|---|
| Documento | Markdown estructurado, reporte |
| Guion | Markdown, PDF futuro, DOCX futuro |
| Storyboard | preview PNG, paquete, video simple futuro |
| Audio | WAV, MP3, segmentos ZIP futuro |
| Proyecto | `.docupodcast.json` + assets, ZIP futuro |
| Observabilidad | reporte Markdown/JSON |

## Paquete de proyecto

Debe contener:

```text
input/
editable/
output/
assets/
jobs/
reports/
logs/
```

Si el proyecto nació desde Word, el DOCX fuente debe viajar en `input/`.

## No prometer

- No mostrar MP3 si no hay encoder.
- No mostrar video si no hay exporter real.
- No mostrar PDF si no hay exporter implementado.
- No exportar podcast completo si faltan segmentos, salvo modo parcial explícito.
