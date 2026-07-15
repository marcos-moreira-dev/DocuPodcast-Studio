# VOICE-CHUNKS-HF1 — GPU honesta, chunk desde fragmento y progreso del documento

## Motivo

Durante la prueba visual posterior a `VOICE-REGISTRATION-WIZARD1` se detectaron tres problemas operativos:

1. La prueba de GPU para Voz IA avanzada podía verse como error aunque el resultado real fuera simplemente que PyTorch existe pero CUDA no está disponible dentro del Python local.
2. El usuario necesitaba una acción explícita para generar audio desde el fragmento seleccionado, sin esperar a renderizar desde el inicio de un documento largo.
3. La barra de estado no mostraba en qué porcentaje aproximado del documento se encontraba el fragmento seleccionado.

## Decisiones

- Voz local simple ya no se declara CPU-only: recibe el dispositivo seleccionado y puede usar GPU si su runtime local lo soporta; si no, el runtime puede ignorarlo o fallar con diagnóstico propio.
- Voz IA avanzada usa smoke CUDA como ruta segura en automático; en dispositivo específico intenta el dispositivo solicitado.
- `Seguir generando` conserva chunks ya generados y reanuda un job recuperable.
- `Rehacer chunks` inicia un job desde el comienzo de la lectura preparada.
- `Renderizar desde aquí` genera audio desde la oración o bloque seleccionado.
- El porcentaje `Documento N%` se calcula según el bloque seleccionado dentro de la lista de bloques normalizados del documento: el primer bloque es 0% y el último 100%.
- El índice lateral del documento debe seguir la selección actual. Si el bloque exacto no existe en el árbol, se selecciona la sección/encabezado más cercano anterior.

## Alcance implementado

- Se corrigió el mensaje de Configuración para que CUDA no disponible sea una prueba completada con CPU honesta, no un error genérico.
- Se añadió botón de barra de estado `Renderizar desde aquí`.
- Se renombró el botón global a `Rehacer chunks` para separar esa acción de `Seguir generando`.
- Se añadió `Documento N%` en la barra de estado.
- `DocumentIndexPanel` sincroniza el árbol con la selección actual del documento sin disparar navegación recursiva.
- Se reforzó el texto de Vista Voces para evitar nombres técnicos en UI normal y para indicar que Voz local simple recibe el dispositivo seleccionado sin prometer aceleración si su runtime no la soporta.

## Validación esperada

- Diagnóstico completo verde.
- Al probar GPU sin CUDA local, la ventana debe decir que la app usará CPU sin mostrar `Error o interrupción`.
- Al seleccionar un fragmento en una página avanzada del documento, el botón `Renderizar desde aquí` debe generar desde ese punto.
- Tras cancelar un job, `Seguir generando` debe reanudar conservando chunks completados.
- El índice lateral debe resaltar la región cercana al fragmento actual.
