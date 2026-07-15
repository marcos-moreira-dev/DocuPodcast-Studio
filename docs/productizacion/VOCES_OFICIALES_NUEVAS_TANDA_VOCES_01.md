# Voces oficiales nuevas - Tanda Voces-01

## Decision

DocuPodcast Studio incorpora 11 voces avanzadas nuevas como presets oficiales incluidos de la aplicacion. No son voces importadas por usuario y no reemplazan la voz predeterminada global.

## Alcance

- Las muestras viven en `samples/voices/advanced-presets`.
- Cada voz tiene 38 WAV, uno por tono/emocion soportado por `VoiceReferenceTone`.
- El catalogo `OfficialAdvancedVoicePresetCatalog` registra los perfiles y sus `VoiceReferenceSampleSet`.
- Proyectos existentes reciben los presets faltantes al abrirse mediante el backfill del lector JSON.

## Voces incluidas

- `hombre_40_firme_latam_personaje`
- `hombre_40_conversacional_latam_dialogo`
- `mujer_20_entusiasta_latam_personaje`
- `mujer_20_suave_colombiana_neutral_dialogo`
- `hombre_35_intenso_explosivo_personaje`
- `hombre_45_resignado_oscuro_personaje`
- `hombre_45_moralista_comico_personaje`
- `hombre_35_aspiracional_emotivo_personaje`
- `hombre_40_indignado_testigo_personaje`
- `mujer_40_dramatica_indignada_personaje`
- `mujer_60_sabia_memoria_latam_personaje`
- `nino_10_terco_caricaturesco_personaje` (incorporacion posterior como preset oficial incluido)

## Contrato de producto

- La vista Voces debe mostrarlas como voces avanzadas predisenadas listas.
- La biblioteca resultante contiene 27 voces avanzadas oficiales y el narrador simple inicial.
- El usuario puede asignarlas a documento, teatro o narrativa cuando el motor Voz IA avanzada este operativo.
- No se agrega flujo de importacion manual para estos archivos.

## Continuacion

La siguiente tanda natural es mejorar la organizacion visual de la biblioteca de voces cuando la lista crece: filtros por genero, edad aproximada, region, rol narrativo y estado de muestras.
