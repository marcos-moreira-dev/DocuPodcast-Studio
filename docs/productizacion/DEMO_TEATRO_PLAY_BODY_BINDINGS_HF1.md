# DEMO-TEATRO-PLAY-BODY-BINDINGS-HF1

## Objetivo

Corregir el demo teatral para que los visuales se asocien solo al cuerpo de la obra y no al titulo, subtitulo o notas finales del Word.

## Decisión de producto

El Word del demo puede conservar titulo, subtitulo y notas operativas, pero las imagenes del demo solo pertenecen a la obra:

- Personajes.
- Escena 1 a Escena 3.
- Dialogos del Narrador, Capitan Bigote y Teniente Tornillo.
- Cierre del Narrador.

Las notas finales no son parte narrable/visual del demo teatral y no deben recibir imagenes preasignadas.

## Cambios

- `ExampleVisualBindingDescriptor` ahora soporta `anchorText`.
- `ClasspathExampleProjectCatalog` define 25 bindings en orden: presentacion de personajes + fragmentos 01 a 24.
- `ExampleVisualBindingWorkflow` busca primero por ancla textual en el documento antes de caer al indice legacy.
- El DOCX demo actualiza la nota final para declarar que las imagenes ya vienen asociadas solo al cuerpo de la obra.
- `DocxDocumentImporter` distingue negrita parcial de prefijo de personaje frente a negrita estructural de titulo/subtitulo.

## Criterio de aceptacion

- `fragmento_01` no se asigna al titulo ni al subtitulo.
- `imagen_01_presentacion_personajes.png` se asigna al bloque `Personajes`.
- `fragmento_24` se asigna al cierre del Narrador.
- Las notas de demo no reciben imagenes.
- Las lineas con `CAPITAN BIGOTE:`, `TENIENTE TORNILLO:` o `NARRADOR:` en negrita parcial no se detectan como subsecciones solo por tener prefijo en negrita.
