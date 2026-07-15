# T121-V07 — Fallback de tonos faltantes en Documento

## Objetivo

Cuando un fragmento/oración solicite un tono que la voz seleccionada no tiene grabado, la app debe usar la muestra neutral de la misma voz y avisar de forma clara.

## Caso

Voz: María  
Tono pedido: Heroica  
Muestra disponible: no  
Muestra neutral: sí

Resultado:

- mostrar aviso;
- usar `María — Neutral`;
- continuar generación/reproducción.

## Mensaje sugerido

```text
La voz “María” no tiene grabada la muestra “Heroica”.
DocuPodcast usará la muestra neutral de María para este fragmento.
```

Opciones:

- `Aceptar`
- `No volver a mostrar para esta voz y tono`

## Reglas

- Si falta tono pero existe neutral, no bloquear.
- Si falta neutral, la voz está incompleta y no se puede usar.
- Para voz prediseñada, si los tonos son copias provisionales de neutral, no avisar cada vez; solo si el usuario espera tonos reales.
- Documento debe mostrar el tono elegido y el tono usado si hubo fallback.

## Use cases

- `ResolveVoiceToneReferenceUseCase`
- `InspectMissingToneFallbackUseCase`
- `RememberToneFallbackNoticePreferenceUseCase`

## Tests recomendados

- `VoiceToneFallbackUsesNeutralTest`
- `MissingToneShowsUserNoticeSourceTest`
- `MissingNeutralBlocksAdvancedVoiceTest`
- `DocumentSidebarShowsToneFallbackSourceTest`

## Criterios de aceptación

- La generación no se rompe por tono faltante.
- El usuario sabe qué ocurrió.
- No se promete tono inexistente.
