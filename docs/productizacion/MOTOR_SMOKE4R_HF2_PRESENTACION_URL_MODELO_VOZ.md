# MOTOR-SMOKE4R-HF2 — etiqueta visible de modelo sin nombres técnicos

## Motivo

El diagnóstico `20260606-121332` dejó `mvn compile` verde, pero `mvn test` falló por guardarraíles de lenguaje visible: `SettingsDialog` mostraba la cadena `Página del modelo XTTS-v2` dentro de la UI de Configuración. Aunque la descarga ya había sido corregida para no usar `/resolve/main` como página, la etiqueta seguía exponiendo el nombre técnico del motor/modelo en presentación.

## Cambios

- `SettingsDialog` ahora muestra `Página oficial del modelo de voz`.
- La ayuda visible ya no menciona `/resolve/main`; dice que no se peguen enlaces directos de descarga.
- Se mantienen internamente las constantes técnicas necesarias para construir las URLs de archivos del modelo.
- Se actualizan los source tests de URL para exigir lenguaje humano en presentación.

## Alcance

No cambia generación, descarga, playback, Vista Voces ni Documento. Es un hotfix de presentación y guardarraíles.

## Validación esperada

```bat
scripts\99-diagnostico-completo.bat
```

El fallo esperado en `VoiceLocalSimpleT121V06SourceTest` y `AdvancedVoiceProgressPf2DSourceTest` debe desaparecer.
