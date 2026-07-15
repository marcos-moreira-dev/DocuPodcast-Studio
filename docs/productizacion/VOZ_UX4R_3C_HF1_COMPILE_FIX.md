# VOZ-UX4R-3C-HF1 — corrección de compilación en Vista Voces

Hotfix aplicado sobre VOZ-UX4R-3C.

## Causa

El diagnóstico local `20260606-095144.zip` reportó fallo de compilación en `VoiceLibraryWorkspaceView.java` por una referencia residual a la variable local `summary` en `homeModule(...)`. Esa variable había dejado de existir al reemplazar el bloque anterior de dashboard por `homeOperationalSummary(...)`.

## Corrección

- `VoiceLibraryWorkspaceView.homeModule(...)` ahora agrega únicamente la lista `list` después de `voiceLibraryHero(...)` y `homeOperationalSummary(...)`.
- `VoiceUx4R3CAntiDashboardSourceTest` queda reforzado para impedir que vuelva la línea residual `box.getChildren().addAll(summary, list);`.

## Alcance

No se cambió comportamiento de motores, playback, Documento, descarga de Voz IA avanzada ni Voz local simple. Es un hotfix de compilación y guardarraíl fuente.

## Validación en entorno ChatGPT

- `VoiceUx4R3CAntiDashboardSourceTest`: OK con stubs de JUnit.
- Suite focal de 5 source tests de Voces: `source-tests-ok=15`.
- `grep` confirmó que la referencia residual ya no existe en código productivo.
- Maven completo no se ejecutó porque `mvn` no está instalado en el entorno ChatGPT.

## Validación recomendada en Windows

Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

El fallo esperado de `cannot find symbol: variable summary` debe desaparecer.
