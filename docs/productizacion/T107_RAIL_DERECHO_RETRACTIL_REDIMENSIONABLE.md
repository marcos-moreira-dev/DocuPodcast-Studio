# T107 — Rail derecho retráctil/redimensionable

## Objetivo

Convertir el rail derecho del Documento en un panel visual útil, plegable y redimensionable, sin mezclarlo con los controles de asignación que pertenecen al sidebar izquierdo.

## Implementado

- `CollapsibleMediaRail` ahora conserva estado plegado/expandido y permite redimensionar el ancho arrastrando el borde izquierdo.
- El rail derecho mantiene su función visual: capas asignadas, storyboard e imágenes disponibles.
- `DocumentMediaRailView` agrega una acción explícita para borrar imágenes del proyecto y limpiar asignaciones visuales.
- La acción destructiva borra assets de imagen, elimina capas `IMAGE`, limpia bindings de storyboard y borra archivos físicos si pertenecen al proyecto.
- `DocumentAudioNarrationPanel` agrega acceso a la biblioteca de voces para administrar voces IA/personajes/muestras.
- `DocumentImageContextPanel` automatiza el guardado del proyecto antes de copiar una imagen al contenedor `.docupodcast`.

## Decisiones de producto

- El rail derecho no configura fragmentos; solo muestra y navega material visual.
- La gestión de voz se centraliza en la biblioteca de voces, no en combos locales interminables dentro del sidebar.
- Las imágenes del usuario deben copiarse dentro del proyecto para mantener rutas relativas.
- Al borrar imágenes desde el panel Visual, se eliminan también los archivos físicos del proyecto, no solo la relación visual.

## Deuda explícita

El ViewModel sigue creciendo por flujos de media. La próxima limpieza técnica debe extraer un `DocumentMediaWorkflowCoordinator` para reducir `DocuPodcastShellViewModel` y encapsular importación, borrado y refresh de assets visuales.
