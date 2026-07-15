# Estrategia — Tanda 14 Guía integrada + recursos IA

La Tanda 14 toma de Domain Model Studio la disciplina de ayuda académica integrada, ayuda operativa separada y recursos IA productizados. La implementación en DocuPodcast no usa `DiagramTypeId`; usa temas y recursos propios del dominio narrativo/audio.

## Decisiones cerradas

1. La guía vive dentro de la app y funciona offline.
2. Word/DOCX es entrada prioritaria en la guía y en la pantalla de ayuda.
3. Recursos IA se exportan como carpeta externa, no quedan mezclados con proyectos del usuario.
4. Plantillas con placeholders no se marcan importables.
5. Ejemplos importables deben declarar contrato `docupodcast-script-v1`.

## Próxima etapa

La Tanda 15 debe enfocarse en packaging/release candidate: app-image/MSI, smoke manual, scripts finales, documentación de instalación y revisión de deuda conocida.
