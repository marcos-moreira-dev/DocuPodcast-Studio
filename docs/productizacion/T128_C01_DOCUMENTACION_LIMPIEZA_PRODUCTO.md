# T128-C01 — Documentación de limpieza de producto

## Objetivo

Alinear la documentación raíz y el handoff con las decisiones consolidadas de CAT-01: DocuPodcast es una aplicación de lectura de documentos, no una herramienta de guiones; Markdown entra como documento; Whisper/STT fue retirado; las superficies heredadas quedan en cuarentena; `PreparedReadingProjection` es la frontera de lectura preparada.

## Reglas consolidadas

- Documento es la raíz del producto.
- El usuario abre Word/DOCX, PDF con texto nativo, Markdown o TXT.
- La aplicación prepara lectura y genera audio.
- Guion no es categoría visible del usuario.
- Markdown no se importa/exporta como guion.
- Whisper/STT/audio a texto no pertenecen al producto ni al build principal.
- Storyboard no es workspace principal; los visuales viven como panel/rail del Documento.
- Voces es biblioteca/gestión de voces.
- Configuración prepara motores y preferencias.

## Documentación actualizada

- `README.md` agrega estado vigente T128-C01.
- `AI_HANDOFF.md` agrega reglas para futuros agentes.
- `VALIDATION.md` registra la validación esperada de la limpieza documental.
- `REGISTRO_TANDAS_DOCUPODCAST.md` registra T128-C01.

## Nota sobre documentación histórica

El repositorio conserva documentos históricos de tandas anteriores que pueden mencionar Guion, Whisper o Storyboard como deuda pasada. Esos documentos no deben tomarse como contrato vigente si contradicen README, AI_HANDOFF, VALIDATION o esta tanda. Las tandas futuras deben actualizar o mover documentación histórica cuando toque cerrar RC final.

## Criterios de aceptación

- Los documentos raíz contienen la regla vigente de producto limpio.
- No presentan Whisper/STT como futuro.
- No presentan Guion como producto visible.
- Futuros agentes tienen una fuente clara para continuar sin reabrir decisiones descartadas.
