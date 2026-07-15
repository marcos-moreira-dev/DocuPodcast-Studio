# 177 — TI5 TextAnchor fuerte y reconciliación

Implementa el primer reconciliador determinista de anchors del proyecto:

- crea anchors ricos desde selección documental;
- confirma `CURRENT` cuando el texto sigue en el mismo rango;
- marca `RELOCATED` cuando aparece una única coincidencia en otro bloque/rango;
- marca `NEEDS_REVIEW` para anchors legacy o coincidencias ambiguas;
- marca `ORPHANED` cuando el texto desaparece.

Esta tanda no modifica el documento fuente y no usa IA/OCR.
