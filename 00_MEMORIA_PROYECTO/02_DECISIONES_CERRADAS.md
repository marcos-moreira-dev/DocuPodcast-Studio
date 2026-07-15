# Decisiones cerradas

Este archivo enumera decisiones ya discutidas y aceptadas para evitar reabrir debates innecesarios.

## Plataforma

- Java 21.
- Eclipse Temurin 21 como distribución objetivo.
- Maven con Toolchain.
- JavaFX como tecnología principal de UI.
- Aplicación de escritorio autocontenida.

## Producto

- Word/DOCX es entrada prioritaria desde el MVP.
- Markdown es puente humano/IA, no la fuente principal del usuario.
- PDF se soportará como entrada secundaria porque puede perder orden lógico.
- El documento original no se modifica.
- El guion narrable es el artefacto central.
- El storyboard vivo usa imágenes aportadas por el usuario.
- El audio se genera por segmentos, no como documento completo de una vez.

## Arquitectura

- DMS será referencia principal para shell, tabs, toolbar, SideDock, workspaces, tema claro, guía, persistencia, Markdown/IA y tests.
- Fractal será referencia principal para jobs largos, progreso, cancelación cooperativa, batch y métricas.
- DocuPodcast tendrá dominio propio, no dominio de diagramas ni fractales.
- El motor TTS estará detrás de `AudioGenerationGateway`.
- La UI no invoca TTS directamente.
- Presentation no importa infrastructure directamente.

## Persistencia

- Proyecto editable: `.docupodcast.json`.
- Recursos pesados: carpeta del proyecto con assets/jobs/exports.
- No se guardan audios binarios dentro del JSON principal.
- Las rutas de assets del proyecto deben ser relativas.
- Jobs de audio deben ser reanudables.

## Voces

- Deben existir voces prediseñadas.
- El usuario podrá usar su propia voz.
- Se podrán registrar voces autorizadas de otras personas.
- No se debe fomentar clonar voces sin consentimiento.
- Estilos emocionales son intención de interpretación, no garantía universal.

## Storyboard

- El storyboard no genera imágenes automáticamente.
- El usuario decide qué imagen corresponde a qué segmento/rango.
- MVP: una imagen por segmento.
- No prometer película ni animación compleja en el MVP.

## Exportación

- Exportar según artefacto real: guion, audio, storyboard, proyecto, diagnóstico.
- No ofrecer formatos si no existe cadena real de implementación y prueba.
