# Importación y exportación teatral

Importación: resolver carpeta/ZIP → validar manifiesto y archivos → parsear Markdown estricto → resolver referencias → crear plan → materializar `TheatreProjectLayer` → reconciliar bindings → resolver todos los snapshots → publicar staging → presentar GUI. Un fallo anterior al commit conserva el proyecto original.

Exportación: recopila bindings reales del proyecto, copia assets, calcula hashes/tamaños, serializa `obra.teatro.md` y crea manifiesto schema 2. Exportar e importar debe conservar los snapshots canónicos salvo timestamps y paths temporales.

`theatre-v1` sigue siendo aceptado por el parser legado. El formato recomendado para intercambio y ZIP es exclusivamente v2.

