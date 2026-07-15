# TI4 — Bloques visuales no narrables

## Objetivo

TI4 consolida la regla de producto para imágenes, tablas y bloques matemáticos/fórmulas dentro del documento fuente:

```text
bloque visual fuente ≠ narración automática ≠ storyboard automático
```

DocuPodcast Studio debe mostrar lo que detecta en el documento, pero no debe obligar a narrarlo ni a incluirlo en video. La decisión creativa queda en manos del usuario.

## Decisiones de producto

1. Las imágenes detectadas en Word/DOCX se tratan como `IMAGE_NOTICE`.
2. Las tablas detectadas se tratan como `TABLE_NOTICE`.
3. Los bloques matemáticos/fórmulas detectados en Word/OMML se tratan como `MATH_NOTICE`.
4. Esos tres tipos son `sourceVisual()`.
5. Los tres son no narrables por defecto.
6. No se convierten automáticamente en storyboard.
7. Si el usuario no les asigna visual, no entran al video.
8. Si el usuario les asigna visual, el RenderUnit puede convertirlos en unidad visual silenciosa.

## Alcance de LaTeX/OMML

TI4 identifica fórmulas/bloques matemáticos de Word, pero no renderiza LaTeX ni OMML dentro de la aplicación.

Esta decisión es intencional. Word puede representar ecuaciones con Office Math Markup Language, objetos, imágenes, campos, MathType u otras variantes. Renderizar eso bien requeriría un motor específico. En esta etapa basta con:

- detectar que existe un bloque matemático/fórmula;
- mostrarlo como bloque visual fuente identificado;
- no narrarlo por defecto;
- dejarlo listo para asignación visual futura;
- documentar que el render visual real queda como capacidad posterior.

## Implementación

### Dominio

`DocumentBlockType` agrega:

```java
MATH_NOTICE("Matemática/fórmula")
```

y define:

```java
sourceVisual()
narratableByDefault() == false para IMAGE_NOTICE, TABLE_NOTICE y MATH_NOTICE
```

### Importación DOCX

`DocxDocumentImporter` identifica descendientes Word/OMML como:

```text
oMath
oMathPara
f
rad
sSup
sSub
nary
```

Cuando los detecta, crea un bloque `MATH_NOTICE` con metadata:

```text
source = docx-math
visualBlock = true
storyboardAssignment = user-controlled
renderPolicy = identified-only
```

### Documento / GUI

`DocumentWorkspaceView` usa el componente transversal `SourceVisualBlockView` para imágenes, tablas y fórmulas. No se hardcodea un bloque visual local en la vista.

`DocumentStructurePanel` agrega filtro de fórmulas.

### Guion

`BuildNarrationScriptUseCase` deja fuera estos bloques porque ya no son narrables por defecto:

```text
IMAGE_NOTICE
TABLE_NOTICE
MATH_NOTICE
```

## Resultado

El sistema queda preparado para TI5/TI6/TI7 y para el flujo de RenderUnit:

```text
Documento + capas → RenderUnitPlan → audio / visual / silencio / omisión
```

TI4 no resuelve todavía la asignación directa de capas a todos los bloques no narrables; deja cerrada la semántica de documento y evita que el sistema siga reforzando la regla antigua de “imagen/tabla = texto que se lee”.


Frase de contrato fuente: imagen / tabla / fórmula no se narran por defecto y no entran automáticamente al storyboard.
