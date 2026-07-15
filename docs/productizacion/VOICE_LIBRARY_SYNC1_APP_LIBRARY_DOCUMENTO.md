# VOICE-LIBRARY-SYNC1 — Biblioteca de voces de la app y sincronización con Documento

## Propósito

Esta tanda cierra el flujo transversal de voces después de `VOICE-REGISTRATION-WIZARD1`.
La Vista Voces no es un dashboard decorativo ni un reproductor de clips fijos: es la biblioteca operativa donde el usuario registra voces y muestras de referencia para que Voz IA avanzada pueda sintetizar texto nuevo.

## Reglas implementadas

1. Las muestras de voz de usuario se gestionan en la biblioteca de voces de la app/runtime, no dentro de cada carpeta de proyecto.
2. El proyecto guarda la referencia de voz/tono para poder usarla, pero no convierte esas muestras en assets locales del proyecto cuando el repositorio de muestras es de aplicación.
3. Las muestras `María · Neutral`, `María · Enojada`, etc. son referencias para generar texto futuro con Coqui/XTTS; no son clips fijos que se reproducen siempre igual.
4. Documento escucha cambios en `activeVoiceLibraryProperty()` y refresca los ComboBox.
5. Documento solo muestra tonos realmente registrados para la voz elegida.
6. Voz local simple y Voz IA avanzada conservan caminos separados: el usuario puede escoger voz IA o audio del computador por fragmento.

## Cambios técnicos

- `ApplicationRuntimeLayout` agrega `voiceLibraryRoot()` y `voiceSamplesRoot()`.
- `InfrastructureServicesFactory` instancia `LocalVoiceSampleFileRepository(runtimeLayout.applicationRoot())`.
- `LocalVoiceSampleFileRepository` soporta dos modos:
  - modo legacy/test: `voices/samples/` dentro del proyecto;
  - modo producto: `voice-library/samples/` bajo el root de la app/runtime.
- `ImportVoiceSampleUseCase` ya no añade la muestra como `ProjectAssetReference` del proyecto cuando el repositorio es de aplicación.
- `VoiceReferenceSample.fileUri()` conserva la ruta física gestionada para que playback, prueba de voz y generación puedan resolver la muestra.
- `VoiceSampleWorkflowCoordinator` puede reproducir, descargar o eliminar muestras aunque no estén registradas como assets de proyecto.
- Las grabaciones hechas con Java Sound se importan como `VoiceSampleOrigin.RECORDED_IN_APP`.

## Criterio de salida

- Registrar/importar muestra no copia el WAV dentro de la carpeta del proyecto cuando se usa el wiring productivo.
- Documento ve la voz/muestra recién registrada sin reiniciar la app.
- En Documento, si María tiene solo Neutral/Feliz/Triste, el ComboBox muestra solo esos tonos.
- El proyecto no falla por no tener esas muestras como assets internos.

## Validación recomendada

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Crear/abrir un proyecto guardado.
3. Ir a Vista > Voces > Gestionar voces.
4. Crear una voz avanzada y registrar/importar Neutral.
5. Volver a Documento, seleccionar un fragmento y confirmar que la voz aparece.
6. Agregar Feliz o Triste y confirmar que el ComboBox de tonos se refresca con esa emoción.
