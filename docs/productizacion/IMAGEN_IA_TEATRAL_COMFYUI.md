# Imagen IA teatral local y ComfyUI

## Decision vigente

La generacion de imagen teatral local usa ComfyUI como runtime configurable. La app puede preparar carpetas, descargar paquetes publicos con confirmacion e importar modelos/runtime/workflows locales, pero no debe descargar modelos pesados en silencio.

El usuario debe entender tres acciones distintas:

- Descargar modelo: baja un checkpoint conocido desde el proveedor configurado.
- Importar modelo local: copia o registra un paquete/modelo que el usuario ya tiene en disco.
- Importar runtime/workflow/componente: trae ComfyUI, workflows, adaptadores, ControlNet, upscalers o piezas tecnicas que no son el checkpoint principal.

## Perfil de memoria

Tanda 100 agrega un perfil persistente de memoria para Imagen IA teatral:

- `SAFE_LOW_VRAM`: modo seguro/bajo consumo. Conserva compatibilidad con `lowVram=true` y usa `--lowvram` si el runtime lo soporta o si no se pudo ejecutar el probe.
- `NORMAL`: no fuerza flags de memoria.
- `VRAM_RAM_OFFLOAD`: opt-in explicito para modelos grandes. Usa VRAM y permite offload a RAM del sistema cuando el runtime ComfyUI soporta flags compatibles; sera mas lento en equipos con poca VRAM.
- `HIGH_MEMORY`: opt-in para equipos con GPU grande. Pide uso agresivo de VRAM completa cuando el runtime lo soporta.

La app no hardcodea flags fragiles para alto consumo. `ComfyUiRuntimeCapabilityProbe` intenta ejecutar `main.py --help` con el Python local del runtime y `ComfyUiLaunchArgumentPlanner` agrega solo argumentos soportados, por ejemplo `--normalvram`, `--cpu-vae` o `--highvram`.

Si el probe no esta disponible, el modo alto consumo no agrega flags agresivos. El modo seguro mantiene el comportamiento heredado para no romper instalaciones existentes.

## Diagnostico esperado

El diagnostico debe exponer informacion accionable:

- perfil de memoria seleccionado;
- valor legacy `lowVram`;
- endpoint local;
- modelo esperado y ruta local;
- estado de descarga/importacion;
- runtime detectado;
- flags ComfyUI reconocidos cuando el probe fue posible;
- errores HTTP/token/acceso restringido cuando Hugging Face no permita descarga directa.

Para modelos Flux o paquetes de 22-24 GB:

- no hay descarga silenciosa;
- si el proveedor exige acceso, se debe indicar token/importacion local;
- el usuario puede intentar ejecucion lenta solo al activar `VRAM_RAM_OFFLOAD` de forma explicita;
- si el runtime no soporta flags de memoria adecuados, se informa como limitacion del runtime, no como fallo ambiguo.

## Alcance por modo

La generacion de imagen pesada pertenece al flujo creativo teatral/narrativo, no al motor PDF visual. Los PDFs pueden servir como referencia visual de solo lectura, pero los guiones editables recomendados para Teatro y Video narrativo son DOCX o Markdown.

## Fuera de alcance

- entrenamiento de LoRA dentro de la app;
- gestion de tokens Hugging Face desde UI en esta tanda;
- guarantees de que un modelo de 24 GB ejecute en todo equipo;
- generacion de video IA a 30 FPS;
- descarga automatica de Flux sin confirmacion y acceso autorizado.
