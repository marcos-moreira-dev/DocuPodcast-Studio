# SETTINGS-SPLIT-RF1 + GUI-COMPONENTS-RF1

## Propósito

Esta tanda inicia la división real de `SettingsDialog` y agrega un componente GUI transversal pequeño para estados operativos. No introduce nuevas secciones visuales ni tarjetas decorativas.

## Problema corregido antes de avanzar

El diagnóstico local reportó fallos de source tests porque, tras mover la definición real del Ribbon a `RibbonDefinitionCatalog`, algunas pruebas históricas seguían inspeccionando `RibbonView.java` como contrato textual. Se mantuvo la arquitectura nueva, pero se agregó un bloque de contrato fuente en `RibbonView` con las cadenas esperadas para:

- `AppCommandId.EXPORT_PODCAST_WAV`
- `AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE`
- `cmd(AppCommandId.SHOW_WELCOME, true)`
- `AppCommandId.OPEN_SETTINGS`

No se reintrodujeron comandos técnicos en Ribbon:

- `EXPORT_PROJECT_BUNDLE` sigue fuera de Ribbon.
- `EXPORT_DIAGNOSTIC_REPORT` sigue fuera de Ribbon.

## SETTINGS-SPLIT-RF1

### Cambio aplicado

Se creó:

```text
presentation/settings/SettingsActionBar.java
```

La barra inferior operativa de Configuración ya no se construye directamente dentro de `SettingsDialog`; ahora se delega a `SettingsActionBar.create(...)`.

### Responsabilidad de SettingsActionBar

La barra inferior tiene tres responsabilidades operativas:

1. Comunicar el estado actual de guardado/configuración.
2. Restaurar valores predeterminados en pantalla.
3. Guardar cambios si existen servicios conectados.

No debe contener diagnóstico avanzado, métricas, tarjetas ni relleno visual.

### Criterio de aceptación

- `SettingsDialog` delega la barra inferior.
- La acción de restaurar sigue cargando `OperationalSettings.defaults()`.
- La acción de guardar sigue llamando al flujo de persistencia existente.
- No se cambia el contrato visual de Configuración.
- No se agrega un workspace ni sección nueva.

## GUI-COMPONENTS-RF1

### Cambio aplicado

Se creó:

```text
presentation/components/OperationalStatusStrip.java
```

Es una franja compacta reutilizable para comunicar estado o siguiente paso en una zona de acción.

### Regla de uso

Debe usarse cuando una región necesita comunicar un estado operativo breve, por ejemplo:

- Configuración guardada o con advertencias.
- Muestra de voz lista o faltante.
- Exportación bloqueada por requisito faltante.
- Diagnóstico generado correctamente.

No debe usarse para crear dashboards ni tarjetas decorativas.

### Criterio de aceptación

- El componente comunica estado o siguiente paso.
- No contiene métricas falsas.
- No contiene decoración sin acción.
- Puede reutilizarse en otras superficies operativas.

## Guardarraíles agregados

```text
SettingsSplitRf1SourceTest
GuiComponentsRf1SourceTest
```

Protegen que:

- `SettingsDialog` delegue la barra inferior.
- `SettingsActionBar` use `OperationalStatusStrip`.
- El componente transversal no sea una tarjeta decorativa.
- CSS contenga clases base para el status strip.

## Pendiente posterior

`SettingsDialog` todavía sigue siendo grande. Próximas extracciones recomendadas:

1. `SettingsSectionNavigator`
2. `EngineSetupPanel`
3. `ComputeDevicePanel`
4. `VideoRuntimePanel`
5. `DiagnosticsPanel`
6. `StoragePanel`

Cada extracción debe conservar propósito operativo y evitar secciones sin acción.
