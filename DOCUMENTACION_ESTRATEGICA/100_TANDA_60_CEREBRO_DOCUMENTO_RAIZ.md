# Estrategia — Tanda 60

T60 fija una corrección conceptual clave: en DocuPodcast Studio V1 el objeto padre es el **Documento narrable**. El guion no desaparece, pero queda definido como proyección interna/avanzada de narración y compatibilidad Markdown, no como raíz paralela para el usuario normal.

La app debe abrir Word/DOCX, PDF, Markdown/MD o TXT para leer y escuchar en modo fuente inmutable. Las capas, audio, storyboard, transcripciones, jobs y exportaciones viven en el proyecto DocuPodcast.

El storyboard es opcional: vincula imágenes a textos/rangos/segmentos y, al renderizar video, la imagen debe durar lo que dure el texto hablado asociado.

Esta tanda no aplica rediseño visual ni refactor profundo. Prepara T61: refactor prioritario del cerebro con coordinadores, manteniendo comportamiento visible y tests verdes.
