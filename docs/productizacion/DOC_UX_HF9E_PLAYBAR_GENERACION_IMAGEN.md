# DOC-UX-HF9E — playbar desacoplado, generación en status bar e imagen sincronizada

## Objetivo

Cerrar las correcciones UX detectadas después de HF9D sin avanzar todavía a Voces, Coqui ni video.

## Cambios

- El panel izquierdo **Imagen** resuelve la miniatura por el mismo rango de frase que usa el rail derecho Visual.
- La barra flotante de lectura mantiene el botón morado, ahora con borde blanco fuerte para mejorar contraste sobre el glass oscuro.
- El playbar queda enfocado en reproducción: pausar/detener ya no cancela la generación de audio.
- La barra de estado expone acciones separadas para generación: **Seguir generando** y **Cancelar generación** cuando corresponde.
- Se agrega botón de reproducción desde el inicio del documento.
- Los botones de fragmento anterior/posterior se deshabilitan en primera/última frase preparada.
- Anterior/posterior reproducen la cue exacta destino, no vuelven a disparar la frase actual por usar la selección anterior.
- Si el motor de voz no está disponible y se necesita generar audio, la UI muestra una confirmación humana para abrir Configuración.

## Validación focal

- Compilación focal de capas domain/application con `javac --release 21`.
- Source tests focales ejecutados con stubs JUnit: 13 OK.
- `DocuPodcastShellViewModel` queda en 2445 líneas, bajo guardarraíl RF2.

## Pendiente

Coqui/Voz IA avanzada queda sectorizado para MOTOR-SMOKE4R: descargar, verificar, seleccionar y generar WAV real.
