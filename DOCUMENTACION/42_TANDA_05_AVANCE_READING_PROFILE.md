# Tanda 5 avance — Reading Profile / reglas de lectura

Esta tanda adelantó la base de perfiles de lectura.

## Implementado

- `domain.reading.ReadingProfile`.
- `HeadingDetectionRules`.
- `ImageNarrationPolicy`.
- `TableNarrationPolicy`.
- `CreateDefaultReadingProfileUseCase`.
- `ApplyReadingProfileUseCase`.
- `ReadingProfileApplicationServices`.
- Panel `DocumentReadingProfilePanel` en el SideDock del Documento.

## Perfil inicial

El perfil por defecto es `Documento académico Word` y prioriza:

- estilos Word `Heading 1` / `Título 1` como títulos;
- estilos Word `Heading 2` / `Título 2` / `Subtítulo` como subtítulos;
- listas Word como listas;
- texto breve en negrita como posible subtítulo;
- imágenes sin descripción como ignoradas para audio por defecto.

## Limitaciones conscientes

La UI todavía no permite crear perfiles personalizados completos. Ese cierre queda para la siguiente tanda.

Falta:

- diálogo/editor de Reading Profile;
- persistencia de perfiles;
- previsualización antes/después más rica;
- reglas por tamaño de fuente si el importador DOCX expone ese dato;
- integración directa con creación del guion narrable.
