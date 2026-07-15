# T121-V10 — Tests, documentación y smoke visual de Voces

## Objetivo

Cerrar la vista Voces con guardarraíles para que no vuelva a convertirse en pantalla placeholder o mezclada con Documento.

## Tests de producto

- `VoiceLibraryFinalContractSourceTest`
- `VoiceLibraryNoFragmentAssignmentSourceTest`
- `VoiceLibraryNoCoquiVisibleSourceTest`
- `VoiceLibraryPiperMinimalSourceTest`
- `VoiceLibraryAdvancedVoiceWizardSourceTest`
- `VoiceToneCatalogExtendedSourceTest`
- `VoiceSampleDownloadSourceTest`
- `VoiceToneFallbackSourceTest`

## Tests de dominio/aplicación

- `VoiceReferenceToneCatalogTest`
- `CreateVoiceProfileUseCaseTest`
- `ImportVoiceReferenceSampleUseCaseTest`
- `RecordVoiceReferenceSampleUseCaseTest`
- `DownloadVoiceReferenceSampleUseCaseTest`
- `DeleteVoiceReferenceSampleUseCaseTest`
- `GenerateVoiceTestUseCaseTest`
- `ResolveVoiceToneReferenceUseCaseTest`

## Smoke visual manual

Crear:

```text
docs/testeo/SMOKE_VISUAL_VOCES_FINAL.md
```

Debe validar:

1. Abrir Vista > Voces.
2. Confirmar que no aparece `Coqui`.
3. Confirmar que no aparece asignación a segmento.
4. Ver voz neutral prediseñada.
5. Crear voz `María`.
6. Grabar muestra neutral.
7. Importar muestra feliz.
8. Descargar muestra feliz.
9. Eliminar muestra feliz.
10. Generar prueba con texto editable.
11. Ver fallback si falta tono heroico.
12. Cambiar a voz local simple.
13. Confirmar que Piper no muestra tonos.
14. Cambiar a modo prueba.
15. Confirmar que no promete voz real.

## Documentación

Actualizar:

- `README.md`
- `AI_HANDOFF.md`
- `VALIDATION.md`
- `REGISTRO_TANDAS_DOCUPODCAST.md`
- `docs/productizacion/T121B_CONTRATO_FUNCIONAL_FINAL_VOCES.md`
- `docs/productizacion/T121D_WIZARD_VOCES_AVANZADAS_CATALOGO_TEATRAL.md`
- guía de usuario final.

## Criterios de aceptación

- Tests pasan.
- Smoke visual aprobado.
- La vista Voces queda documentada.
- La app no promete capacidades falsas.


## Tests adicionales para frases guía

Agregar:

- `VoiceTonePromptCatalogSourceTest`
- `VoiceRecordingWizardShowsTonePromptSourceTest`
- `VoiceRecordingCancelDoesNotReplaceSampleTest`
- `VoiceRecordingSaveAssociatesSampleWithToneTest`

Validaciones:

- Cada tono tiene frase guía.
- La frase se muestra antes de grabar.
- Cancelar no reemplaza muestra anterior.
- Guardar asocia el audio al tono correcto.
- Las frases no contienen contenido explícito.


## Estado implementado en T121-V10

- Se corrige el source test de V04C para respetar los componentes extraídos en V09.
- Se agrega smoke visual final en `docs/testeo/SMOKE_VISUAL_VOCES_FINAL.md`.
- Se agregan guardarraíles fuente para contrato final, documentación y lenguaje visible.
- La Vista Voces queda cerrada como biblioteca de voces/muestras/pruebas, no como asignador de fragmentos.
