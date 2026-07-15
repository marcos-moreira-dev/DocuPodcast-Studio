# 25 — Tanda 2 implementada: persistencia base

Esta nota estratégica registra que la persistencia mínima ya pasó de diseño a código.

## Implementado

- Agregado raíz `DocuPodcastProject`.
- Metadata de proyecto.
- Tipos/estado de proyecto.
- Catálogo inmutable de assets.
- Referencias relativas seguras.
- Repositorio `.docupodcast.json`.
- Reader/writer JSON.
- Versionado de formato.
- Validación de payload por tipo de proyecto.
- Familias iniciales de servicios.
- Tests unitarios.

## Frontera preservada

```text
Word/DOCX = input prioritario.
Markdown = puente IA/humano.
.docupodcast.json = proyecto editable.
Assets/jobs = archivos pesados.
```

## Tanda nueva agregada

Se agrega:

```text
Tanda 2.5 — Integración UI de proyecto/session.
```

Motivo: la persistencia ya existe en capas internas, pero el usuario necesita abrir/guardar desde la app antes de que el importador DOCX tenga una sesión real donde colocar el documento.

