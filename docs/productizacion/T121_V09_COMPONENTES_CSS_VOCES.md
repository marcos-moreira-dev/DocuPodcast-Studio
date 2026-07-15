# T121-V09 — Componentes/CSS de Voces

Base: T121-V08 con diagnóstico local rojo por un source test de V06 que esperaba el literal `no usa muestras humanas`.

## Objetivo

Consolidar la Vista Voces como superficie de producto basada en componentes reutilizables y CSS específico, sin regresar a tarjetas, filas o botoneras improvisadas.

## Cambios implementados

- Corrige el texto de Voz local simple para conservar el contrato exacto `no usa muestras humanas`.
- Agrega componentes específicos de Voces:
  - `VoiceProfileCard`
  - `VoiceEngineModeCard`
  - `VoiceToneBadge`
  - `VoiceSampleRow`
  - `VoiceGeneratedTestPanel`
- `VoiceLibraryWorkspaceView` consume los nuevos componentes para:
  - tarjetas de modo;
  - tarjetas de perfil/readiness;
  - filas de muestras por tono;
  - panel de prueba generada;
  - prueba local simple.
- `VoiceGeneratedTestPanel` reutiliza componentes transversales `SectionHeader` y `ActionBar`.
- `VoiceToneBadge`, `VoiceProfileCard` y `VoiceEngineModeCard` reutilizan primitives transversales como `InfoBadge`/clases de `AppStyles`.
- Se actualiza `GuiComponentCatalog` para registrar los componentes nuevos de Voces.
- Se amplía `voice-library.css` con clases específicas para los componentes nuevos.

## Contrato de UX

La interfaz gráfica mantiene nombres amigables:

- `Voz IA avanzada`
- `Voz local simple`
- `Modo de prueba`

No se reintroducen nombres técnicos de motores en strings visibles de presentación.

## Tests

- `VoiceComponentsCssT121V09SourceTest`
- Ajuste compatible con `VoiceLocalSimpleT121V06SourceTest`

## Validación en entorno ChatGPT

- `javac --release 21` de `domain + application + infrastructure`: OK.
- Compilación focal de `VoiceLibraryWorkspaceView` y componentes de Voces con stubs JavaFX: OK.
- Compilación focal de tests V06/V09 con stubs JUnit: OK.
- Ejecución reflexiva de 7 métodos de tests fuente V06/V09: OK.
- Maven completo no ejecutado por falta de `mvn` en este entorno.

## Próxima tanda recomendada

T121-V10 — Tests, documentación y smoke visual de Voces.
