# Roadmap posterior a T81G

## Estado actual

La navegación de DocuPodcast Studio ya está organizada así:

- Menú común/típico.
- Configuración solo desde menú bar.
- Documento limpio.
- Barra flotante de lectura.
- Inspector izquierdo contextual.
- Rail derecho de miniaturas/medios.
- Toolbar con iconos y grupos.
- Vistas avanzadas degradadas a herramientas.

## Pendiente principal

La siguiente fase es **T82 — Release Candidate**, con foco en estabilización, evidencia, packaging, smoke manual y documentación de instalación/uso.

## Riesgos que T82 debe revisar

- Que el flujo real `Abrir documento → escuchar` sea comprensible con motor mock y con motor real configurado.
- Que configuración explique cómo conectar TTS/STT/FFmpeg sin invadir Documento.
- Que el RC no prometa OCR, editor Word completo, nube ni video avanzado.
- Que los assets se guarden dentro de la carpeta contenedora.
- Que la navegación no vuelva a exponer Guion/Audio/Voces/Storyboard como pantallas principales.
