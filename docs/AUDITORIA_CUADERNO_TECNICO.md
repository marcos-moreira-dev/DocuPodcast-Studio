# Auditoría UX y arquitectura: cuaderno técnico

## Alcance actual

Problema Técnico Express y el flujo normal ya comparten el mismo `TechnicalProblemDialog`, el perfil `document-problem`, la entrada de ratón/tableta, el lienzo por teselas, el historial y la exportación. Express cambia la entrada y la salida del flujo, pero no mantiene un segundo editor. Esta es la base correcta para evolucionar ambos como un solo cuaderno técnico.

## Hallazgos

1. La barra separaba herramientas relacionadas entre dos superficies: los modos aparecían arriba y sus opciones debajo. El usuario debía interpretar estados como “Panear/dibujar” en vez de elegir una herramienta concreta.
2. Lápiz y borrador existían, pero faltaba una herramienta geométrica básica. Una línea recta es necesaria para diagramas, ejes, vectores y construcciones.
3. La medición de ángulos debe ser una guía temporal. Convertirla directamente en tinta haría difícil corregir o repetir una medición y contaminaría la exportación.
4. Las acciones destructivas usan iconos parecidos. Deben conservar texto o ayudas inequívocas, además de estado deshabilitado cuando no exista un objetivo.
5. El lienzo creciente funciona como una hoja larga, no como un cuaderno. Una columna de miniaturas sin un modelo de páginas daría una falsa sensación de persistencia: todas las miniaturas apuntarían al mismo lienzo.
6. El perfil transversal declaraba lápiz, borrador, imagen, recorte y paneo. Las nuevas herramientas también deben declararse allí para que cada editor muestre solo lo que su perfil admite.

## Barra recomendada

- **Herramienta:** Lápiz, Línea, Medir ángulo, Borrador, Panear, Seleccionar región.
- **Trazo:** color, fondo, grosor y muestra del grosor.
- **Vista:** zoom y resultado de la medición temporal.
- **Historial:** deshacer, rehacer, limpiar trazos y limpiar lienzo.
- **Imágenes y regiones:** interacción, tamaño, recorte, copiar, pegar, mover y eliminar.

Solo una herramienta principal puede estar activa. El botón activo debe verse seleccionado y cada herramienta debe tener nombre, icono, ayuda y foco de teclado.

## Modelo necesario para páginas y miniaturas

La siguiente etapa debe introducir un `TechnicalNotebook` persistente con una lista ordenada de `TechnicalNotebookPage`. Cada página necesita identidad estable, título opcional, tamaño, fondo, estado de tinta, imágenes y miniatura derivada. El problema técnico guardado debe referenciar el cuaderno o una página, en vez de serializar un único lienzo monolítico.

El visor recomendado es una columna izquierda plegable con miniaturas, número de página, botón para añadir, duplicar, renombrar, reordenar y eliminar. El lienzo central muestra una página a la vez. Cambiar de página debe guardar un checkpoint antes de cargar la siguiente.

## Prioridad

1. Herramientas explícitas, línea recta y medición temporal.
2. Atajos y estados accesibles para la barra.
3. Modelo persistente de cuaderno y migración del lienzo actual a Página 1.
4. Visor de miniaturas y gestión de páginas.
5. Formas adicionales: rectángulo, elipse, flecha, regla y cuadrícula ajustable.

