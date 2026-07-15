# T112 — Smoke visual UX / RC visual

## Objetivo

Cerrar la tanda visual de producto con un smoke manual y verificable. T112 no agrega una superficie nueva: estabiliza la experiencia visible tras T110R, T108 y T111, y deja un protocolo claro para decidir si la GUI ya puede pasar a demos/productización.

## Base de entrada

- Base técnica: T111-HF4.
- Hotfix incluido: se restaura el marcador histórico `T111-HF2 — PNG icon polish` en `ribbon.css` para que el source test de iconografía T111 siga verde.
- Diagnóstico de referencia recibido: `20260602-153524`, donde `mvn compile` y smoke cerebro estaban OK, pero fallaba `IconographyCssAndPlaybarT111SourceTest`.

## Resultado esperado

El diagnóstico local debe quedar verde:

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

Resultado esperado:

```text
Maven compile: OK
Maven tests: OK
Smoke automatico cerebro: OK
Preflight arranque motores: OK
Piper y FFmpeg locales: OK
```

## Checklist visual obligatorio

### 1. Inicio

- La app abre en Inicio sin errores visuales.
- El ribbon usa iconos PNG más visibles, no abreviaturas tipo `DOC`, `PLAY`, `GEN`.
- Las acciones iniciales son claras: abrir fuente, nuevo proyecto, abrir proyecto.
- La pantalla no promete STT/Whisper ni workspace técnico.

### 2. Abrir documento fuente

- Abrir DOCX, PDF con texto nativo, Markdown o TXT desde Fuente documental.
- El documento se muestra como fuente de solo lectura.
- Las capas se explican como datos del proyecto, no como edición del Word/PDF.

### 3. Imágenes embebidas DOCX

Criterio: imagen embebida DOCX visible cuando el archivo fuente trae media recuperable.

- Abrir nuevamente el DOCX desde fuente, no un snapshot viejo.
- Las imágenes embebidas deben aparecer en la hoja cuando `DocxDocumentImporter` logra obtener `embeddedImageBase64`.
- Si el proyecto fue creado antes del soporte base64, el repositorio intenta recuperar la imagen desde `source/*.docx`.
- Una imagen fuente no se asigna automáticamente al storyboard.

### 4. Tablas y bloques visuales fuente

- Una tabla del documento se muestra como bloque visual/fuente resumido.
- La tabla no debe convertirse automáticamente en imagen de storyboard.
- LaTeX/fórmulas quedan documentadas como capacidad futura; no deben venderse como implementadas si aún no hay renderer real.

### 5. Playbar flotante

- El botón principal se lee como acción del documento: escuchar documento o reproducir selección.
- Si no hay proyecto guardado, primero aparece aviso/modal explicando que DocuPodcast necesita crear el proyecto.
- Después del aviso se abre el diálogo para elegir ruta/nombre del proyecto.
- Tras guardar, la acción continúa.

### 6. Proceso largo / chunks de audio

- Al preparar audio aparece un overlay compacto.
- El overlay permite cancelar el proceso.
- El overlay permite ocultarse sin cancelar el job.
- La barra de estado muestra acceso para volver a mostrar la preparación, por ejemplo `Mostrar preparación · 118/185 segmentos`.
- La app debe permitir seguir leyendo mientras se generan chunks.

### 7. Pausar, reanudar y detener

- Pausar detiene playback y solicita pausa/cancelación segura del job activo si hay preparación.
- Reanudar intenta retomar playback o job recuperable.
- Detener detiene playback y cancela proceso activo.
- Refrescar contenido no debe pisar un job activo sin pedir cancelación/pausa segura.

### 8. Sidebar izquierdo

- Texto, Audio e Imagen usan iconos PNG transversales.
- Texto muestra fragmento/oración seleccionada.
- Audio permite elegir origen, voz IA/audio local y acceso a biblioteca de voces.
- Imagen permite asignar imagen al fragmento sin modificar la fuente.
- No debe haber controles JavaFX sueltos sin clase/componente transversal.

### 9. Rail derecho

- El rail es visual/navegacional.
- No vuelve la sección redundante `Asignadas`.
- Permite ver storyboard e imágenes.
- Colapsar/expandir usa icono transversal.
- Clic en tarjeta visual actualiza la selección y el sidebar izquierdo.

### 10. Voces

- Voces se siente como biblioteca, no como placeholder técnico.
- Regla visible: Voces administra voces, Documento asigna voces y Configuración prepara motores.
- IDs técnicos no dominan la experiencia.
- Importar/grabar muestra no promete clonación real.

### 11. Guía y configuración

- Guía abre como diálogo renderizado, sin Markdown crudo.
- Guía habla del programa real: fuente documental, proyecto, lectura, voces, imágenes, audio, storyboard, exportación y solución de problemas.
- Configuración concentra motores/rutas/diagnóstico sin volver cabina técnica.
- Whisper/STT no aparece en la guía visible ni en navegación normal.

### 12. Exportación y cierre/reapertura

- Guardar proyecto crea carpeta contenedora.
- Reabrir proyecto recupera documento, capas, voces, storyboard y assets.
- Exportar audio o paquete solo aparece como viable cuando hay contenido preparado.
- Storyboard/video debe comunicarse como resultado de visuales asignados, no de toda la fuente documental.

## Criterio de aprobación visual

T112 queda aprobada si:

- diagnóstico completo verde;
- app abre;
- documento se puede importar;
- imagen DOCX se ve cuando el documento contiene media recuperable;
- guardado guiado aparece antes de preparar audio;
- overlay compacto no bloquea el trabajo;
- iconos PNG son visibles y no parecen placeholders textuales;
- no se observan STT/Whisper ni workspaces técnicos como flujo normal;
- se puede cerrar/reabrir el proyecto sin perder el estado central.

## Limitaciones conocidas que pasan a backlog

- LaTeX/fórmulas visuales quedan para una tanda técnica posterior.
- Tabla como bloque visual fuente aún requiere mejora de representación, pero ya no debe confundirse con storyboard.
- RenderUnit end-to-end queda para la fase técnica posterior.
- FFmpeg como job persistente/cancelable queda fuera de T112.
- Instalador/productización real empieza después del menú Ejemplos o de la fase TP.

## Siguiente tanda

Si T112 queda verde y visualmente aceptable, la siguiente tanda de producto es:

```text
T113 — Menú Ejemplos + proyectos demo internos
```
