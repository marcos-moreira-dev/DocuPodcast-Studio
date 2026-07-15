# Decisiones cerradas

Este archivo lista decisiones ya acordadas. No deberían reabrirse sin una razón técnica fuerte.

## Producto

- El nombre de trabajo es **DocuPodcast Studio**.
- No es un mini Word.
- Es un estudio local de documento, guion, voz, audio y storyboard vivo.
- La frase corta es: **Word → Guion → Voz → Audio → Storyboard vivo**.

## Entrada

- Word/DOCX es entrada prioritaria desde el MVP.
- Markdown es puente humano/IA y formato importable avanzado.
- PDF simple puede soportarse después de DOCX/Markdown/TXT.
- Las imágenes del Word se detectan; no se promete interpretar visualmente la imagen si no trae descripción.

## Arquitectura

- JavaFX será la aplicación visible principal.
- La app debe sentirse autocontenida.
- No habrá API visible ni backend HTTP salvo que sea estrictamente necesario.
- Si se usa Python/TTS internamente, debe quedar encapsulado como worker/gateway, sin exigir instalación manual al usuario final.
- Se usará arquitectura por capas: domain, application, infrastructure, presentation, bootstrap.

## Build

- Java 21.
- Eclipse Temurin.
- Maven con Toolchain.
- JavaFX 21.

## Guion

- El guion narrable es documento estructurado, no canvas.
- Debe permitir selección por segmento y más adelante por rango de texto.
- Debe guardar voz, personaje, estilo, imagen y audio asociados.

## Audio

- La generación no necesita ser en tiempo real.
- Debe ser por segmentos.
- Debe mostrar progreso, ETA, segmento actual, fallidos y logs.
- Debe permitir cancelación cooperativa y reanudación/reintento.

## Voces

- Deben existir voces prediseñadas.
- El usuario podrá usar su propia voz.
- Se pueden usar voces de amigos/amigas solo con autorización.
- No se debe fomentar clonar voces sin permiso.
- Estilos emocionales son intención de interpretación y dependen del motor.

## Storyboard

- Storyboard vivo significa imagen del usuario + audio + texto activo.
- No se promete película automática.
- MVP: una imagen por segmento.
- Futuro: varias imágenes por segmento y exportación de video simple.

## Persistencia

- `.docupodcast.json` + carpeta de assets/jobs es la persistencia editable.
- No guardar WAV/MP3/imágenes como Base64 dentro del JSON.
- Rutas del proyecto deben ser relativas y seguras.

## UI

- Tema claro/luminoso.
- DMS es referencia visual principal.
- Fractal no se copia como tema oscuro.
- SideDock para estructura/propiedades/ayuda.
- Toolbar para acciones frecuentes.
- Workspace central para resultado principal.

## Promesas falsas

- No mostrar botones ni formatos si no existe cadena real implementada.
- No ofrecer MP3 si no hay encoder.
- No ofrecer emoción si el motor no lo soporta.
- No marcar plantillas IA con placeholders como importables.
