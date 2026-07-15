# Checklist — Tanda 58C Smoke exploratorio mínimo

Marcar cada ítem durante la prueba manual.

## Preparación

- [ ] Ejecuté `scripts\02-ejecutar-tests.bat`.
- [ ] Los tests quedaron verdes.
- [ ] Abrí la app con `scripts\01-ejecutar-app.bat`.
- [ ] Usé `samples\smoke\documento-simple-t58c.docx` o documenté otro DOCX usado.

## Proyecto y documento

- [ ] Creé un proyecto nuevo.
- [ ] Importé un DOCX sin error bloqueante.
- [ ] El documento se ve legible.
- [ ] La acción principal de escuchar/reproducir está visible o es fácil de encontrar.
- [ ] No tuve que entrar obligatoriamente en Guion/Audio/Storyboard para empezar.

## Escuchar documento

- [ ] Al pulsar escuchar, la app preparó guion si faltaba.
- [ ] Si necesitó guardar proyecto, el mensaje fue entendible.
- [ ] La app generó audio mock/local por fragmentos.
- [ ] La reproducción inició con buffer inicial.
- [ ] Al faltar un fragmento, el mensaje fue entendible.
- [ ] Pude pausar.
- [ ] Pude reanudar.
- [ ] Pude detener o volver al documento sin perder estado.

## Seguimiento visual

- [ ] El texto activo se resaltó.
- [ ] El scroll mantuvo la lectura en una zona cómoda o al menos no molestó.
- [ ] No aparecieron paneles técnicos bloqueando la lectura.
- [ ] El mini rail no invadió la lectura.

## Guardar y reabrir

- [ ] Guardé el proyecto.
- [ ] Cerré la app.
- [ ] Reabrí la app.
- [ ] Abrí el proyecto guardado.
- [ ] El documento reabrió correctamente.
- [ ] El guion reabrió correctamente o se reconstruyó de forma clara.
- [ ] El audio/manifest/job reabrió correctamente o se explicó su estado.
- [ ] Pude volver a reproducir o entender qué faltaba para reproducir.

## Clasificación final

- [ ] Aprobado: flujo mínimo completado.
- [ ] Condicionado: flujo completado con problemas de UX o mensajes.
- [ ] Fallido: no se pudo completar el flujo mínimo.

## Hallazgos breves

```text
FUNCIONAL:

UX:

ARQUITECTURA:

DOCUMENTACION:

DECISION PARA SIGUIENTE TANDA:
```
