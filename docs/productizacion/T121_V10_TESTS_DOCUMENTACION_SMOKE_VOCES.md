# T121-V10 — Tests, documentación y smoke visual de Voces

## Estado

Implementada sobre T121-V09.

## Alcance

Cierra la serie funcional T121 de Vista Voces con guardarraíles de contrato, documentación raíz actualizada y smoke visual manual. La Vista Voces queda protegida como biblioteca de voces, muestras por tono y pruebas generadas. Documento conserva la asignación a fragmentos/oraciones.

## Corrección incluida

Se ajusta `VoiceLibraryWizardWiringT121V04CSourceTest` para validar la clase CSS `voice-profile-card` en el componente extraído `VoiceProfileCard`, no en `VoiceLibraryWorkspaceView`. Esto alinea el test con T121-V09, donde las tarjetas dejaron de ser markup ad hoc y pasaron a componentes reutilizables.

## Guardarraíles finales

- Voces no asigna fragmentos ni segmentos.
- Voces no muestra personajes, roles ni estilos heredados.
- Voces usa componentes específicos y transversales, no filas/tarjetas improvisadas.
- La GUI usa `Voz IA avanzada`, `Voz local simple` y `Modo de prueba`.
- Los nombres técnicos de motores no aparecen en la interfaz gráfica normal.
- La voz avanzada trabaja con muestras por tono y fallback neutral.
- La voz local simple no muestra tonos, muestras humanas, clonación ni catálogo teatral.
- La prueba generada usa frase editable y reproducción de la última prueba.

## Tests agregados

- `VoiceLibraryFinalSmokeT121V10SourceTest`
- `VoiceLibraryT121V10DocumentationSourceTest`

## Smoke visual

Se agrega `docs/testeo/SMOKE_VISUAL_VOCES_FINAL.md` con checklist manual para validar Inicio > Vista > Voces, muestras por tono, prueba generada, fallback neutral, descarga/eliminación de muestras y modo Voz local simple.

## Validación esperada

- `mvn test` debe quedar verde.
- `scripts\99-diagnostico-completo.bat` debe quedar verde en Windows.
- El smoke visual debe repetirse después de cualquier cambio futuro en Voces.
