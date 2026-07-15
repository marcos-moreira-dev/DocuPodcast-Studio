# Tanda 68 — Rediseño UI aplicado sobre Documento/Inicio

Tanda basada en T67 con hotfix del source test de configuración.

## Corrección previa

`SettingsDialogSourceTest` ahora acepta que `SettingsDialog` reciba `SettingsApplicationServices` al abrirse. La configuración operativa real de T67 requiere ese cableado.

## Cambios visibles

- Documento principal menos técnico.
- Métricas de bloques/narrables/tablas/imágenes fuera de la página de lectura.
- Etiquetas de bloque más humanas.
- Aviso visible de fuente solo lectura + `Refrescar contenido`.
- Bienvenida ampliada a Word, PDF, Markdown y TXT como fuentes de lectura.

## Criterio de salida

La app debe seguir pasando `scripts\02-ejecutar-tests.bat` y conservar el contrato de Documento narrable raíz.
