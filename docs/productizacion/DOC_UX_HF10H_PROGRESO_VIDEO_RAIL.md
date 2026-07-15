# DOC-UX-HF10H — progreso legible, tamaño generado y rail Visual honesto

## Objetivo

Ajuste posterior a DOC-UX-HF10G tras prueba real con documento grande: conservar la mejora de rendimiento y pulir la información visible durante la generación larga de audio.

## Cambios implementados

- El overlay de generación deja de mostrar **tamaño estimado** y muestra **audio generado en disco** calculado sobre los WAV ya creados.
- La ETA muestra horas cuando corresponde: `1 h 59 min 40 s restantes aprox.` en vez de `119 min 40 s`.
- El progress bar del overlay queda más alto y con barra cargada verde para que se lea como avance real.
- El rail Visual conserva la virtualización de HF10G, agrega contador de fragmentos listados y explica que la lista solo renderiza las tarjetas visibles para mantener fluidez.
- Las tarjetas del rail ajustan su ancho al viewport para evitar scroll horizontal y cortes innecesarios.
- Configuración > Video local aclara la diferencia entre URL centralizada e importación desde carpeta: la descarga automática queda preparada como contrato, pero la acción actual copia una carpeta que ya contenga `ffmpeg.exe` y `ffprobe.exe`.

## Decisión de producto

El rail derecho sí debe tener todas las tarjetas lógicas desde que la lectura está preparada, pero no debe crear miles de nodos JavaFX. La solución correcta es `ListView` virtualizado: todos los fragmentos están en la lista, pero JavaFX solo materializa las filas visibles.

## GPU

La app ya modela selección CPU/GPU en Configuración y la Voz IA avanzada puede recibir el argumento de dispositivo a través del wrapper local. Queda pendiente MOTOR-PERF1 para validar uso real de GPU con modelos instalados, caída honesta a CPU y evidencia en smoke. Voz local simple sigue considerándose liviana/neutral y no debe prometer aceleración GPU si el motor no la soporta.

## Validación esperada

- Documento grande: overlay ocultable, contador estable y barra visible.
- Progreso: texto con horas si la ETA supera 60 minutos.
- Tamaño: solo audio generado en disco, sin estimación fluctuante.
- Rail: contador visible, scroll vertical normal, sin scroll horizontal.
- Configuración: Video local comunica claramente qué hace Preparar/Importar y qué papel cumple la URL.
