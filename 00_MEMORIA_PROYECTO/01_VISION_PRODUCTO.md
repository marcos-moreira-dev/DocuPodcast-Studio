# Visión de producto

DocuPodcast Studio es una aplicación de escritorio JavaFX autocontenida que convierte documentos largos, especialmente notas en Word/DOCX, en material escuchable y revisable: guion narrable, audio local por segmentos, podcast final y storyboard vivo con imágenes aportadas por el usuario.

La idea nació como un “mini Word con botón de generar audio”, pero se refinó hacia algo más preciso: un **Document-to-Podcast Studio** y, luego, un **estudio de guion narrable + storyboard vivo**.

## Frase corta

**Word → Guion → Voz → Audio → Storyboard vivo.**

## Para qué sirve

- Estudiar documentos largos con menor fricción.
- Convertir notas de Word en audio local de buena calidad.
- Preparar guiones narrables revisables antes de generar voz.
- Generar audio por segmentos, sin necesidad de tiempo real.
- Reintentar únicamente segmentos fallidos.
- Usar voces prediseñadas, voz propia o voces autorizadas.
- Asociar imágenes a segmentos para crear un storyboard vivo.
- Reproducir texto, audio e imagen de forma sincronizada.

## Qué no es

- No es un editor Word completo.
- No es un clon de Microsoft Word.
- No es un generador automático de películas.
- No es una herramienta para clonar voces sin permiso.
- No promete emociones perfectas si el motor TTS no las soporta.

## Usuarios previstos

- Estudiante que guarda apuntes en Word y quiere escucharlos como podcast.
- Persona que prepara guiones de teatro y quiere voces/personajes.
- Autor que quiere un storyboard vivo simple con imágenes propias.
- Usuario técnico que quiere controlar archivos, jobs, manifests y exports.

## Principio de alcance

La app puede ser tecnológicamente compleja por dentro, pero debe sentirse simple por fuera. JavaFX será la app principal visible. Cualquier motor de IA, TTS o proceso pesado debe quedar encapsulado y no obligar al usuario a configurar Python, servidores o APIs manuales si no es estrictamente necesario.
