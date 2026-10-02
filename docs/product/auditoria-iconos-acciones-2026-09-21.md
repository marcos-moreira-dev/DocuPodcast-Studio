# Auditoría de iconos de acciones — 2026-09-21

## Hallazgo principal

Las capturas de Objetos y Mapa espacial y acciones muestran una X adicional fuera del botón y un lápiz pequeño junto al icono de edición. El recorrido de código confirma la causa:

1. `TheatreObjectsPanel`, `TheatreCharactersPanel` y `TheatreSceneFoldList` crean botones con texto `X` o `✎` y un tamaño fijo de 24 × 24.
2. `ActionButtonFactory` llama a `SemanticActionIcons.decorate`, que añade el SVG correspondiente.
3. El modo de presentación predeterminado de JavaFX muestra gráfico y texto. Ambos compiten por los 24 píxeles y el glifo de texto aparece junto al icono.

`TheatreSceneFoldList` se utiliza tanto en el mapa espacial como en el mapa textual. El defecto no depende de la obra cargada; reducir el espacio disponible hace más evidente el problema. Las capturas por sí solas no prueban un fallo del zoom.

## Corrección transversal

`SemanticActionIcons` presenta solo el gráfico cuando reemplaza los símbolos `X` o `✎`. Conserva el texto subyacente, los tooltips, los nombres accesibles y los manejadores. Las etiquetas descriptivas como «Eliminar escena» siguen mostrando icono y texto.

Si una acción cambia de símbolo a etiqueta mediante `refresh`, se recupera su modo de presentación anterior. La decoración continúa respetando los gráficos explícitos existentes. No se aplica una regla CSS global que oculte el texto de todos los botones.

## Otros consumidores auditados

La revisión de llamadas de la fábrica y del decorador encontró también seis acciones de voces con símbolos incorporados en su etiqueta, además del icono semántico: grabar, detener, cancelar, escuchar, repetir y escuchar voz de prueba. Se retiraron esos prefijos, incluyendo los textos enlazados del estado de reproducción.

Las flechas de ordenación por lotes y los símbolos de tamaño de la ventana de procesos no reciben un icono semántico para esas etiquetas; no presentan esta misma duplicación. Los botones de imagen de las cabeceras de teatro ya usan `iconOnly` con texto vacío.

## Verificación y límites

- Prueba JavaFX con CSS real, botones de 24 × 24 y escalas del contenedor de 0,78, 1 y 1,25: no se dibuja el glifo antiguo y el gráfico queda dentro del botón.
- Comprobación de tooltip/nombre accesible y ejecución de ambas acciones.
- Comprobación de etiquetas normales, gráficos personalizados y transición de símbolo a texto.
- `SemanticActionIconsTest`, `SemanticActionButtonLayoutTest` y `StudioControlContractTest`: cinco pruebas satisfactorias.

Comando: `mvn -pl studio-desktop -am test -Dtest=SemanticActionIconsTest,SemanticActionButtonLayoutTest,StudioControlContractTest -Dsurefire.failIfNoSpecifiedTests=false`.

La verificación cubre controles renderizados por JavaFX; no equivale a recorrer manualmente toda la aplicación ni a probar todos los DPI de Windows. La instancia ya abierta debe reiniciarse con la compilación actualizada para reflejar el cambio.

## Ampliación: Estudio documental

Se revisaron las vistas de contenido y ajustes del video documental, el panel y diálogo de problemas técnicos, el índice, las acciones de imagen/audio, las miniaturas y los controles compartidos para ocultar/restaurar paneles.

No se encontró el patrón de teatro (texto `X`/`✎` más SVG en un botón compacto) en esas vistas activas. `TechnicalProblemDialog.iconToolButton` usa texto vacío y `GRAPHIC_ONLY`; `SidePanelToggleButton` vacía el texto al cambiar el icono; las miniaturas usan presentación gráfica. Los botones descriptivos del video mantienen intencionalmente icono y etiqueta.

Se encontró un caso relacionado en `DocumentStructurePanel`: su etiqueta incluye un marcador del tipo de bloque y texto del documento, pero la fábrica infería un icono de acción a partir de ese texto. Un bloque ignorado con «Cerrar el circuito» podía mostrar su marcador `×` más un icono de cierre; «Editar una imagen» podía añadir un lápiz. Se elimina el gráfico inferido para estas filas y se conserva el marcador y la selección del bloque. No se encontraron llamadas de creación de este panel en el código actual: es una corrección preventiva de un componente existente, no evidencia de un defecto visible en el recorrido activo.

Se añadieron comprobaciones de que las palabras del documento no generen iconos de acciones y de que las herramientas compactas reales del diálogo de problemas tengan un solo gráfico, texto vacío y nombre accesible. La ampliación se verifica junto con las pruebas existentes de video documental, disponibilidad del panel lateral, paneles plegables y botones semánticos. Esta revisión de código y controles no sustituye una inspección manual de todas las pantallas.

Resultado de la ampliación: compilación correcta y 15 pruebas satisfactorias, sin errores ni omisiones. Registro local: `target/document-icon-audit-validation.log`.
