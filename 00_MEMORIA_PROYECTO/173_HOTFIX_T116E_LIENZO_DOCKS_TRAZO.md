# Hotfix T116E - Lienzo, docks colapsados y trazo mas fluido

## Que se implemento
- El crecimiento del lienzo por scroll ahora puede repetirse: al llegar al borde se pide 25% mas de espacio y el scroll retrocede a 78% para dejar margen visible.
- Se agrego guard contra reentrada durante el crecimiento del lienzo para evitar cascadas de eventos del scrollbar.
- El ancho maximo seguro del lienzo subio a 6 columnas de tiles, manteniendo el modelo por mosaicos acotados.
- El trazo del lapiz ahora usa segmentos cuadraticos suavizados en vez de lineas rectas entre eventos de mouse/tableta.
- El borrador tambien usa curvas y sigue limpiando solo la capa de tinta mediante alpha, sin pintar color de fondo sobre imagenes.
- En modo dibujo o seleccion de region del canvas, los scrollbars quedan ocultos/no operables; en modo panear vuelven a habilitarse.
- Al crear, abrir, cerrar o adjuntar documento a un proyecto, el rail derecho y el playbar desplazado vuelven a estado colapsado.
- El SideDock izquierdo del documento ahora se crea colapsado por defecto.

## Que quedo fuera
- Recortar una imagen ya colocada dentro del canvas y sincronizar ese recorte con la fuente/captura queda para una tanda posterior.
- No se cambiaron OCR, temario PDF ni persistencia `.docupodcast.json`.
- No se modifico Domain Model Studio/UENS.

## Decisiones tecnicas
- Se mantuvo el canvas por tiles para evitar superficies JavaFX gigantes.
- El crecimiento por scroll usa debounce y un flag de supresion temporal, en vez de crecer dentro de la cascada directa del scrollbar.
- Para mejorar la escritura con mouse/tableta se usa interpolacion por puntos medios y curvas cuadraticas.
- Los docks minimizados se aplican desde el ViewModel al cambiar el ciclo del proyecto, no desde cada panel.

## Archivos tocados
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT116ESourceTest.java`

## Tests ejecutados
- `mvn -q -DskipTests compile`
- `mvn -q -Dtest="TechnicalProblemT116ESourceTest,TechnicalProblemT116C117SourceTest,TechnicalProblemHotfixT116BSourceTest,StudyDocumentUxT97SourceTest" test`
- `mvn -q test`

Nota: el primer `mvn -q test` fallo porque `DocuPodcastShellViewModel` quedo en 2609 lineas y el guardarrail exige maximo 2600. Se compacto solo el cambio agregado y la suite completa quedo verde con 2598 lineas.

## Proximos pasos exactos
1. Verificar manualmente el scroll al borde varias veces, tanto horizontal como vertical, con mouse/tableta.
2. Implementar recorte de imagen dentro del canvas como herramienta propia: seleccionar imagen, definir crop, actualizar vista y exportacion.
3. Revisar si el limite de 6 columnas alcanza en ejercicios largos reales o si conviene un limite configurable por memoria disponible.
