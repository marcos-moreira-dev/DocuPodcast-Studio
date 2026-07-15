# Tanda Voces - Nino 10 terco caricaturesco oficial

## Implementado

- Se copiaron 38 archivos `.wav` desde `C:\Users\MARCOS MOREIRA\Downloads\todas voces\nino_10_terco_caricaturesco_personaje` hacia `samples/voices/advanced-presets/nino_10_terco_caricaturesco_personaje`.
- No se eliminaron ni modificaron los archivos de la carpeta origen.
- Se registro la voz como preset oficial incluido en `OfficialAdvancedVoicePresetCatalog`.
- ID estable: `VOC-PRESET-NINO-10-TERCO-CARICATURESCO-PERSONAJE`.
- La biblioteca por defecto pasa a 27 voces avanzadas oficiales mas el narrador simple.

## Decisiones

- La voz queda disponible como muestra oficial de Voz IA avanzada, no como voz predeterminada global.
- Se conserva el nombre de carpeta y los nombres de WAV tal como llegaron.
- El backfill JSON existente incorpora esta voz al abrir proyectos antiguos porque depende del catalogo oficial vigente.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/OfficialAdvancedVoicePresetCatalog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/OfficialAdvancedVoicePresetCatalogTest.java`
- `docs/productizacion/VOCES_OFICIALES_NUEVAS_TANDA_VOCES_01.md`
- `samples/voices/advanced-presets/nino_10_terco_caricaturesco_personaje/*`

## Pendiente

- Verificar visualmente en la vista Voces que aparece como voz avanzada oficial y que muestra sus 38 tonos.
- Proxima tarea natural: mejorar filtros y organizacion visual de la biblioteca de voces cuando el catalogo crece.
