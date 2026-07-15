# Exportaciones

Exportar según naturaleza del artefacto, no según pantalla.

## Mapeo

```text
Guion       → Markdown / PDF / DOCX futuro
Audio       → WAV / MP3
Storyboard  → paquete / PNG preview / video simple futuro
Proyecto    → .docupodcast.json + assets
Diagnóstico → Markdown / JSON
```

## Proyecto bundle

Debe crear:

```text
input/
editable/
output/
assets/
jobs/
reports/
logs/
```

Si el proyecto nació de Word, `input/` debe contener el DOCX original.

## Política de formatos

No ofrecer:

- MP3 sin encoder;
- video sin exporter real;
- PDF sin exporter real;
- podcast completo con segmentos faltantes;
- paquete portable con assets inválidos.
