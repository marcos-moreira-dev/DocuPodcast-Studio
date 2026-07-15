# Tanda 2 - Decisiones Arquitectonicas

Fecha de implementacion documental: 2026-06-24

## Resumen

Estas decisiones fijan el marco de Tanda 2. Son decisiones de documentacion y arquitectura objetivo; no ejecutan refactors ni cambian comportamiento.

## ADR-001: `ProjectMode` es la fuente de verdad de modalidad

Decision: usar `ProjectMode` como vocabulario oficial de modalidad.

Alternativas descartadas:

- Usar `ProjectKind` como modalidad principal.
- Crear un enum nuevo para Teatro/Narrativa/Estudio.
- Inferir modalidad desde workspaces visibles.

Razon:

`ProjectMode` ya declara los tres modos de producto y `ProjectModeCapabilities` ya los convierte en capacidades. `ProjectKind` queda como dato tecnico/legacy.

Compatibilidad:

`ProjectModePolicy` conserva inferencia legacy, incluyendo teatro cuando hay datos teatrales.

## ADR-002: Capacidades transversales antes que ownership por modalidad

Decision: Voces, Visuales y Exportacion son capacidades compartidas.

Alternativas descartadas:

- Que Teatro sea propietario de generacion de imagen.
- Que Narrativa sea propietaria de visuales por fragmento.
- Que cada modalidad tenga su propio exportador/readiness completo.

Razon:

Las tres modalidades pueden necesitar voces, visuales y exportacion. La modalidad aporta significado; la capacidad aporta operacion reusable.

Compatibilidad:

Los datos especificos de Teatro permanecen en `TheatreProjectLayer`, pero los motores y readiness de voz/visual/exportacion deben quedar fuera de Teatro.

## ADR-003: No crear interfaces Java en Tanda 2

Decision: Tanda 2 solo crea contratos Markdown.

Alternativas descartadas:

- Crear interfaces anticipadas de voz, visuales y readiness sin consumidores listos.
- Introducir esqueletos para futuras tandas.

Razon:

El codigo actual ya tiene casos de uso y servicios suficientes para orientar la arquitectura. Crear tipos sin consumidores aumenta superficie y puede endurecer decisiones prematuras.

Compatibilidad:

Si Tandas 6-9 demuestran una necesidad real de puertos nuevos, se crearan con pruebas y consumidores concretos.

## ADR-004: Application contiene readiness; presentation solo presenta

Decision: readiness y reglas de exportacion deben vivir en application.

Alternativas descartadas:

- Calcular readiness en dialogs o coordinadores JavaFX.
- Duplicar bloqueos en Centro de exportacion y workflows.

Razon:

`InspectExportReadinessUseCase` ya centraliza export honesty y puede ser usado por UI, diagnostico y paquetes.

Compatibilidad:

`ExportCenterCoordinator` puede seguir armando targets visuales, pero no debe convertirse en fuente de verdad de requisitos productivos.

## ADR-005: `ApplicationServices` sigue siendo fachada interna

Decision: mantener `ApplicationServices` y `application/services/*` como agrupadores de casos de uso.

Alternativas descartadas:

- Inyectar casos de uso concretos desde presentation por todas partes.
- Crear una fachada global nueva para arquitectura objetivo.

Razon:

La fachada actual ya agrupa Project, Document, Voice, Visual, Theatre, Grammar, Export y otros dominios. Bootstrap ensambla concreciones.

Compatibilidad:

No se agregan dependencias directas de presentation a infrastructure. Las clases grandes de shell no deben crecer; nuevas operaciones deben delegarse.

## ADR-006: El respaldo no es fuente de reemplazo

Decision: no copiar ni restaurar archivos desde el respaldo como estrategia de Tanda 2.

Alternativas descartadas:

- Sobrescribir clases actuales con versiones del respaldo.
- Migrar paquetes por similitud de nombre.

Razon:

Tanda 1 encontro 0 archivos relevantes solo en respaldo y G ya tiene avances de gramatica, readiness y exportacion no presentes alli.

Compatibilidad:

El respaldo sigue disponible como evidencia historica. Cualquier uso futuro debe ser comportamiento por comportamiento, no copia masiva.

## ADR-007: Ribbon queda fuera de implementacion hasta Tanda 8

Decision: Tanda 2 solo declara que el contrato estable es `AppCommandId` + registry/policy/catalog.

Alternativas descartadas:

- Reordenar pestanas en Tanda 2.
- Ocultar/deshabilitar comandos por intuicion antes de cerrar capacidades.

Razon:

El Word maestro indica que ribbon debe esperar a comandos reales recuperados y fronteras de Voces/Visuales. Tanda 1 tambien marco que el ribbon necesita tanda propia.

Compatibilidad:

Los documentos de Tanda 2 pueden citar comandos, pero no cambian UI.

## ADR-008: No cambiar persistencia en Tanda 2

Decision: no modificar `DocuPodcastProject`, `ProjectMetadata`, `TheatreProjectLayer` ni JSON.

Alternativas descartadas:

- Agregar campos de arquitectura objetivo ahora.
- Versionar gramatica o proyecto sin implementacion asociada.

Razon:

La tanda es documental. El roundtrip teatral esta verde y no hay necesidad de schema para expresar el plan.

Compatibilidad:

Cualquier cambio futuro de schema exige migracion, prueba de apertura legacy y prueba de roundtrip.

## Validacion ejecutada

No se ejecutaron builds ni tests en el respaldo. En G se ejecuto:

```powershell
mvn -q "-Dtest=ArchitectureBoundaryTest,ProjectModeTest,ProjectModeCapabilitiesTest,BuildVisualProductionProjectionUseCaseTest,VoiceCapabilityPolicyTest,InspectExportReadinessUseCaseTest,DocuPodcastProjectTheatreJsonTest,ImportProjectGrammarMarkdownUseCaseTest,BuildTheatreProductionProjectionUseCaseTest,ExportCenterCoordinatorTest" test
```

Resultado: PASS, exit code 0, 16.6 s.

| Prueba | Resultado | Cobertura usada para Tanda 2 |
| --- | --- | --- |
| `ArchitectureBoundaryTest` | PASS | Limites domain/application/infrastructure/presentation. |
| `ProjectModeTest` | PASS | Modos oficiales e inferencia legacy. |
| `ProjectModeCapabilitiesTest` | PASS | Capacidades por modalidad. |
| `BuildVisualProductionProjectionUseCaseTest` | PASS | Visuales transversales por fragmento/assets. |
| `VoiceCapabilityPolicyTest` | PASS | Capacidad de voces y motores. |
| `InspectExportReadinessUseCaseTest` | PASS | Readiness comun y salidas por modo. |
| `DocuPodcastProjectTheatreJsonTest` | PASS | Persistencia teatral. |
| `ImportProjectGrammarMarkdownUseCaseTest` | PASS | Importacion de gramatica y semantica. |
| `BuildTheatreProductionProjectionUseCaseTest` | PASS | Proyeccion/readiness teatral. |
| `ExportCenterCoordinatorTest` | PASS | Targets del Centro de exportacion por modo. |

## Archivos creados en esta tanda

1. `DOCUMENTACION_ACTUAL/TANDA_02_ARQUITECTURA_OBJETIVO/TARGET_ARCHITECTURE.md`
2. `DOCUMENTACION_ACTUAL/TANDA_02_ARQUITECTURA_OBJETIVO/MIGRATION_MAP.md`
3. `DOCUMENTACION_ACTUAL/TANDA_02_ARQUITECTURA_OBJETIVO/CAPABILITY_CONTRACTS.md`
4. `DOCUMENTACION_ACTUAL/TANDA_02_ARQUITECTURA_OBJETIVO/ARCHITECTURE_DECISIONS.md`

No se crearon clases, interfaces Java, recursos productivos ni cambios de schema.
