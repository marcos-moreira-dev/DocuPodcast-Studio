# T121-V01 — Deshuesamiento de la vista Voces actual

## Objetivo

Limpiar la vista Voces para que deje de mezclar responsabilidades. Esta tanda no implementa todavía el wizard final ni el catálogo teatral; prepara la superficie para que la siguiente fase pueda reconstruirla sin arrastrar la UX anterior.

## Decisiones aplicadas

- Voces administra voces y muestras.
- Documento asigna voces a fragmentos.
- Configuración prepara motores.
- La vista Voces ya no ofrece `Asignar al segmento seleccionado`.
- La vista Voces ya no muestra `Personajes / roles`.
- La vista Voces ya no muestra `Estilos de interpretación`.
- La vista Voces no muestra `Coqui` ni `XTTS` en la UX.
- La UX usa `Voz IA avanzada`, `Voz local simple` y `Modo de prueba`.
- La vista deja de usar clases CSS heredadas `script-*`.

## Cambios técnicos

- Se simplificó `VoiceLibraryWorkspaceView`.
- Se retiraron combos de personaje, voz y estilo de la vista Voces.
- Se retiró la llamada directa a `assignVoiceToSelectedSegment(...)` desde Voces.
- Se mantuvo la capacidad actual de importar/grabar/detener muestra.
- Se mantuvo `VoiceLibraryCapabilityReport` para estado y validación.
- Se agregaron clases CSS propias de Voces:
  - `voice-library-title`
  - `voice-library-summary`
  - `voice-library-body`
  - `voice-card-title`
  - `voice-card-subtitle`

## Lo que queda para próximas tandas

- T121-V02: modelo de voces, muestras, tonos teatrales y frases guía.
- T121-V03: almacenamiento seguro y descarga de muestras.
- T121-V04: wizard de registro de voz avanzada.
- T121-V05: prueba generada con frase editable.
- T121-V06: Piper modo mínimo.
- T121-V07: fallback de tonos faltantes.
- T121-V08: rediseño visual final.

## Criterios de aceptación

- La vista Voces ya no asigna fragmentos.
- La vista Voces no muestra lenguaje legacy de personaje/rol/estilo.
- La vista Voces no muestra `Coqui` ni `XTTS`.
- La vista usa lenguaje de producto limpio.
- Los tests fuente protegen que no vuelva la deuda retirada.
