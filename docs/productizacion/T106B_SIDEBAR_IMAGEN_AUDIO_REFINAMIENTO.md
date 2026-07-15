# T106B — Sidebar: refinamiento de imagen, audio y reproducción de fragmento

Base: T106A.

Esta tanda corrige observaciones reales de uso antes de pasar al rail derecho T107. No cambia el cerebro narrativo principal; ajusta el puente de interfaz para que el inspector izquierdo se comporte como una herramienta cómoda del documento.

## Cambios de producto

- El módulo **Fragmento** conserva `Reproducir desde aquí` y agrega `Reproducir fragmento`, pensado para reproducir solamente el fragmento seleccionado cuando ya exista audio preparado.
- El módulo **Imagen** muestra un aviso modal cuando el proyecto no está guardado y el usuario intenta importar una imagen, porque las imágenes deben copiarse dentro de la carpeta del proyecto para mantener rutas relativas.
- Al importar imagen, se actualiza inmediatamente la vista previa del módulo Imagen y el panel Visual mediante `documentMediaRevisionProperty`.
- Al quitar una imagen, se elimina la capa del fragmento y, si ningún otro fragmento o binding usa ese asset, se quita también el asset y el archivo físico del proyecto.
- El módulo **Audio** mantiene selección por `ComboBox` para voz IA y estilo. Se amplía el catálogo base de estilos: neutro, cálido, feliz, alegre, triste, calmado, serio y dramático.

## Deuda registrada

`DocuPodcastShellViewModel` volvió a superar el límite transitorio previo por ajustes de imagen/reproducción. El siguiente refactor técnico debe extraer un `DocumentMediaWorkflowCoordinator` o equivalente para mover importación/remoción/refresco de imagen fuera del ViewModel.

## No incluye

- Rail derecho redimensionable.
- Playback real por oración/unidad end-to-end.
- Clonación de voz humana.
- Eliminación de assets compartidos si siguen referenciados por otros fragmentos.
