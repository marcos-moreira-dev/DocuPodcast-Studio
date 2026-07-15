# Tanda 101 - Roadmap Estudio PDF Continuidad

## Que se implemento

- Se creo el roadmap rector `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`.
- El roadmap lista en orden las tandas T102 a T118.
- Se dejo claro que los documentos antiguos `T100`, `T101`, `T102`, etc. dentro de `docs/productizacion` son historicos de otra numeracion y no deben guiar esta linea actual.
- Se fijo el guardarrail de no eliminar ni mover Domain Model Studio/UENS.

## Que quedo fuera

- No se elimino ni movio documentacion historica.
- No se implemento aun seleccion rectangular, OCR, busqueda visual ni reemplazo completo del flujo PDF por regiones.

## Decisiones tecnicas

- La continuidad vigente queda centralizada en un solo documento rector.
- La limpieza documental sera segura: primero etiquetar como historico, luego archivar fisicamente solo si una tanda futura valida referencias.
- DMS/UENS queda explicitamente fuera de cualquier limpieza.

## Archivos tocados

- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`
- `00_MEMORIA_PROYECTO/151_TANDA_101_ROADMAP_ESTUDIO_PDF_CONTINUIDAD.md`

## Tests ejecutados

- `mvn -q "-Dtest=RoadmapStudyPdfPostT100Test,PdfVisualWorkspaceT102SourceTest,RenderPdfVisualPageUseCaseTest" test`
- `mvn -q test`

## Proximos pasos

1. T102: activar visor PDF visual central cuando la fuente documental sea PDF.
2. T103: cache y rendimiento PDF.
3. T104: seleccion rectangular sobre PDF visual.
