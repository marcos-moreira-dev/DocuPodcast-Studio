# UX-HF6 + DOWNLOAD-HF5 — Build verde y descarga visible de Voz IA

Esta tanda corrige el bloqueo de compilación reportado en `20260604-151214.zip` y cierra una mejora de producto para la descarga de Voz IA avanzada.

## Correcciones

- `DocuPodcastShellViewModel` ya no usa variables mutables dentro de lambdas de búsqueda de `PlaybackCue`.
- `SettingsDialog` agrega resumen detallado para Voz local simple sin mezclar tipos de reporte.
- El ViewModel vuelve a quedar bajo el límite RF2 de líneas.

## Descarga visible

Cuando la preparación o configuración inicial intenta descargar Voz IA avanzada y no queda lista, la ventana muestra el detalle del recurso pendiente en lenguaje humano, incluyendo errores HTTP cuando existan. Ya no se pierde el detalle detrás de “1 componente pendiente”.

## Criterio de producto

La UI normal no debe mostrar rutas internas ni nombres técnicos. Los detalles de soporte pueden existir en diagnóstico, pero el flujo principal debe indicar qué falta de forma comprensible.
