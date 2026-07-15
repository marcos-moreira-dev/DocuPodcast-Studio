# Roadmap post T80 — cerebro V1 congelado

T80 cierra el ciclo de cerebro previo al rediseño frontal. A partir de aquí no se debe seguir agregando complejidad funcional de núcleo salvo hotfixes de build verde o bugs críticos detectados por tests.

## Siguiente paso inmediato

### Lectura 15 — frontend quirúrgico antes de rediseñar

Objetivo: leer con lupa las superficies de presentación que se van a simplificar sin romper los contratos de cerebro ya congelados.

Archivos probables:

```text
MainToolbarView
DocumentWorkspaceView
DocumentMediaRailView / CollapsibleMediaRail
SettingsDialog
GuideDialog / guía integrada
WorkspaceDescriptorCatalog
WorkspaceToolbarActionProvider
CSS de shell/document/toolbar/sidedock/settings
capturas actuales
```

Criterios:

```text
Documento debe ser lector narrado, no cabina de avión.
Capas debe ser contextual y simple.
Configuración puede ser bodega técnica.
Guion, audio jobs, render y diagnóstico deben quedar en avanzado.
Componentes transversales obligatorios.
```

## T81 — Rediseño frontal guiado

Foco:

```text
pantalla Documento más limpia;
toolbar compacta;
rail de capas colapsado/contextual;
estados humanos de reproducción;
metadatos técnicos ocultos por defecto;
guía renderizada como ayuda de producto;
configuración básica/avanzada.
```

Guardarraíles esperados:

```text
MainReaderDoesNotExposeTechnicalVocabularySourceTest
DocumentMetadataNotDumpedToReaderSourceTest
MediaRailCollapsedByDefaultSourceTest
MainToolbarCompactModeSourceTest
SettingsShowsPresetsBeforeRawCommandsSourceTest
GuideRendersReadableHelpSourceTest
```

## T82 — Release Candidate

Foco:

```text
app-image/MSI;
hashes;
licencias;
smoke manual final;
smoke automático del cerebro;
paquete de herramientas/modelos documentado;
limitaciones V1 visibles y honestas.
```

## Regla de gobierno post T80

Toda nueva capacidad debe pasar por una de estas rutas:

```text
1. Hotfix: corrige rojo o bug crítico sin cambiar contrato V1.
2. Frontend: mejora presentación respetando BrainV1CapabilityMatrix.
3. V2: se documenta para después del RC.
```

No se deben agregar promesas nuevas al usuario antes de T82.
