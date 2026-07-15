# Tanda 59C — Contrato de documentos solo lectura

## Tipo

Documental / arquitectura protegida / producto.

## Base

T59B verde validada localmente por el usuario.

## Objetivo

Dejar escrito como regla de producto para la versión actual que los documentos fuente se abren en modo solo lectura: Word/DOCX, PDF, Markdown/MD y TXT.

La app puede crear guion, capas, audio, storyboard, transcripciones y exportaciones, pero esos artefactos viven dentro del proyecto DocuPodcast. El archivo fuente no se edita ni se sobrescribe.

## Cambios principales

Se agregan:

```text
docs/productizacion/CONTRATO_DOCUMENTOS_SOLO_LECTURA_V1.md
docs/productizacion/REFERENCIA_OFIMATICA_WORD_LIKE.md
docs/productizacion/ROADMAP_POST_T59C_DOCUMENTOS_CEREBRO.md
docs/referencias/ui/wps-office-wordlike-reference-2026-05-30.png
```

Se agregan guardarraíles fuente:

```text
SourceDocumentsReadOnlyContractSourceTest
OfficeLikeUiReferenceSourceTest
```

## Decisión central

```text
Documento fuente = evidencia/origen inmutable.
Proyecto DocuPodcast = lugar donde viven guion, capas, audio, storyboard, transcripciones y metadatos.
```

## Alcance

Aplica a:

- Word/DOCX actual;
- PDF cuando se implemente como documento fuente;
- Markdown/MD cuando se abra como documento fuente;
- TXT cuando se implemente como documento fuente.

Markdown importable como `docupodcast-script-v1` sigue siendo un artefacto del proyecto si se importa mediante acción explícita de guion. Eso no autoriza editar el archivo Markdown original.

## Referencia visual futura

La captura de WPS/Word-like se conserva como referencia de patrones útiles: página centrada, grupos de acciones, herramientas superiores, estado inferior y zoom. No implica copiar una suite ofimática ni convertir DocuPodcast en editor de texto completo.

## Próximo paso

T60 — auditoría ejecutable del cerebro de la app, ahora incluyendo como regla que ningún flujo debe tratar documentos fuente como editables.


## Frase de guardarraíl

El documento fuente debe tratarse como fuente inmutable durante la versión actual.
