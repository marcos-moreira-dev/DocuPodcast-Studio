# VOZ-UX4R-3D — Catálogo teatral de tonos en Vista Voces

## Objetivo

Completar la gestión de tonos/emociones en la Vista Voces sin volver al modelo de dashboard ni a tarjetas pesadas. La vista debe servir a un usuario teatral: puede elegir Neutral, tonos recomendados y el catálogo teatral extendido desde un ComboBox, y cada tono se gestiona como una muestra de referencia real.

## Cambios aplicados

- `VoiceSampleWorkflowCoordinator.registrationPlan(...)` ahora pide el catálogo teatral extendido al construir el plan de registro.
- `VoiceLibraryWorkspaceView` carga en `tonePromptSelector`:
  - Neutral.
  - Tonos recomendados.
  - Catálogo teatral extendido.
- El selector de tono usa `ComboBox` con prompt humano y más filas visibles para no esconder emociones.
- La vista agrega una sección sobria `Catálogo teatral de tonos` con filas, no tarjetas ni dashboard.
- La sección explica que Documento solo mostrará emociones que ya tengan muestra registrada para esa voz.
- Los tonos se mantienen como muestras de referencia: no son instrucciones de texto ni promesas de expresividad exacta.

## Contrato de producto

Neutral sigue siendo obligatoria para que una voz aparezca como utilizable en Documento. Las emociones teatrales son opcionales y se registran una por una mediante grabación o importación. Importar o grabar un tono reemplaza la muestra anterior de ese mismo tono; cancelar conserva lo previo.

## Validación focal

Guardarraíl agregado:

```text
VoiceUx4R3DTheatricalToneCatalogSourceTest
```

Protege que el catálogo teatral extendido esté conectado, que el selector sea ComboBox, que la vista use filas sobrias y que no se reintroduzcan dashboards de tonos.

## Fuera de alcance

- No modifica motores ni descarga de Voz IA avanzada.
- No toca playback.
- No toca Documento.
- No agrega nombres técnicos de motores a la UI normal.
