# Tanda Voces-01 - Voces oficiales nuevas

## Que se implemento

- Se copiaron 11 carpetas de voces desde `C:\Users\MARCOS MOREIRA\Downloads\todas voces` hacia `samples/voices/advanced-presets`.
- Cada carpeta destino conserva 38 archivos WAV.
- La carpeta fuente no se elimino ni se movio.
- Se registraron las 11 voces en `OfficialAdvancedVoicePresetCatalog` como presets oficiales XTTS.
- La biblioteca por defecto pasa a 26 voces avanzadas oficiales mas el narrador simple.
- Se agrego cobertura para que proyectos antiguos con biblioteca parcial reciban los presets oficiales faltantes al abrirse.

## Que quedo fuera

- No se cambio la voz predeterminada global.
- No se creo flujo de importacion manual para estas voces.
- No se modifico Domain Model Studio/UENS.
- No se cambio la UI de la vista Voces en esta tanda.

## Decisiones tecnicas

- Las voces nuevas usan los nombres de carpeta originales como `sourceFolder`.
- Los IDs son estables con prefijo `VOC-PRESET-`.
- Los nombres visibles se dejaron en ASCII para evitar problemas de codificacion en esta base.
- Se reutiliza `VoiceSampleOrigin.APP_DEFAULT` y `VoiceFileOwnership.APP_RESOURCE`.

## Archivos tocados

- `samples/voices/advanced-presets/*`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/OfficialAdvancedVoicePresetCatalog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/OfficialAdvancedVoicePresetCatalogTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectVoiceLibraryJsonTest.java`
- `docs/productizacion/VOCES_OFICIALES_NUEVAS_TANDA_VOCES_01.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=OfficialAdvancedVoicePresetCatalogTest,DocuPodcastProjectVoiceLibraryJsonTest" test`
- `mvn -q test`

La suite completa quedo verde. Durante `mvn -q test` aparecieron advertencias de PDFBox sobre offsets de streams en fixtures PDF, sin fallar pruebas.

## Proximos pasos exactos

1. Ejecutar compile y tests completos.
2. Probar visualmente la vista Voces y confirmar que aparecen las 11 voces nuevas.
3. Planificar filtros de biblioteca de voces por genero, edad aproximada, region, rol y estado de muestras.
