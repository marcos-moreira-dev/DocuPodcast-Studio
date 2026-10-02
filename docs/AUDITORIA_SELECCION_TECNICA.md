# Selección y transformación del editor técnico

## Hallazgos y cambios

- La ruta antigua de región hacía snapshotRegion, borraba la región y añadía una imagen. Eliminada de copiar, pegar, mover y eliminar región.
- Técnico normal y Express reutilizan CanvasStrokeSelection. La selección intersecta los trazos y conserva sus puntos, presión, color y grosor. No crea PNG para transformar tinta.
- El marco tiene tiradores de escala y giro; se puede arrastrar también por el espacio interior del marco. Supr elimina la selección; las operaciones crean un checkpoint de deshacer.
- El giro de objetos era de 90 grados: un rombo simétrico parecía inmóvil. Ahora es de 15 grados y conserva el centro.

## Límites

- Las formas insertadas previamente como imágenes siguen siendo imágenes; esta corrección preserva la naturaleza vectorial de los trazos manuscritos.
- La selección transforma trazos enteros intersectados, no fragmentos recortados.
- La región admite una selección mixta de tinta, imágenes y formas. El marco común mueve, escala, gira, copia y elimina los elementos conservando la tinta vectorial. Las imágenes mantienen su propia representación.
- «Editar imágenes y formas» permanece habilitado; al activar Seleccionar región se marca para incluir objetos. Puede desmarcarse para seleccionar únicamente tinta.
- Verificación automatizada de transformaciones, conservación de puntos y ausencia de imágenes nuevas. Falta validación manual con una tableta física.
