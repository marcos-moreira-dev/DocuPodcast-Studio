# Tanda 2 - Contratos de Capacidades

Fecha de implementacion documental: 2026-06-24

## Alcance

Este archivo define contratos de producto y arquitectura en Markdown. No crea interfaces Java. Los contratos sirven para que Tandas 3-9 implementen adaptadores incrementales sin duplicar motores ni romper persistencia.

## Convenciones

| Termino | Significado |
| --- | --- |
| Modalidad | Tipo vertical de proyecto definido por `ProjectMode`. |
| Capacidad | Servicio transversal reusable por modalidades. |
| Target semantico | Elemento de una modalidad que recibe voz, visual, audio, readiness o exportacion. |
| Referencia | Id estable hacia voz, estilo, asset, fragmento, escena, objeto, personaje o intervencion. |
| Motor | Implementacion concreta local o externa, siempre detras de application/infrastructure. |

## Contrato: resolucion de modalidad

Owner actual: `ProjectModePolicy`.

Entrada minima:

- `DocuPodcastProject`.
- `ProjectMetadata.mode()`.
- `ProjectMetadata.kind()` como compatibilidad.
- Datos teatrales para inferencia legacy.

Salida:

- Un `ProjectMode` oficial.

Reglas:

1. Si hay datos teatrales, el modo efectivo es `THEATRE_PRODUCTION`.
2. Si no hay modo explicito, `ProjectKind` solo ayuda a inferir legacy.
3. Presentation debe consultar el modo efectivo; no debe crear heuristicas paralelas.
4. Readiness/exportacion debe basarse en modo efectivo, no solo en texto visible de UI.

## Contrato: asignacion de voz

Owner actual: `VoiceApplicationServices` y dominio consumidor.

Entrada minima:

- Target semantico: segmento, personaje, intervencion u otro target aprobado.
- `characterId` cuando aplique.
- `voiceProfileId`.
- `performanceStyleId` o tono equivalente.
- `VoiceLibrary`.

Salida:

- Modelo de la modalidad actualizado con referencias de voz.
- Diagnostico si la combinacion no existe o no es usable.

Reglas:

1. El target guarda ids, no rutas ni comandos de motor.
2. La validacion de perfiles/tonos pertenece a Voces.
3. La modalidad define que significa la asignacion: narrador, personaje, intervencion, etc.
4. Teatro puede usar `VoiceRoleAlias`, pero no debe conocer XTTS, Piper ni Java Sound.
5. Cambios de texto, voz o tono deben invalidar audio de forma explicita en la tanda que toque generacion.

## Contrato: generacion de voz/audio

Owner actual: application audio/render/voice + infrastructure gateways.

Entrada minima:

- Script o render units preparados.
- Referencias de voz/estilo resueltas.
- Settings/runtime de motor.
- Politica de intentos y estado de jobs.

Salida:

- Jobs/snapshots de audio.
- Segmentos/chunks completos, pendientes, fallidos o cancelados.
- Readiness de audio para playback/exportacion.

Reglas:

1. La generacion es una capacidad transversal, no de Teatro.
2. La UI puede iniciar, cancelar o consultar jobs, pero no construir comandos de motor.
3. Exportacion solo consume audio existente/readiness; no sintetiza faltantes silenciosamente.
4. Los gateways concretos viven en infrastructure y se ensamblan en bootstrap.

## Contrato: asignacion visual

Owner actual: `FragmentAssetBinding`, `ProjectAssetCatalog`, `VisualProductionApplicationServices` y dominios consumidores.

Entrada minima:

- Target semantico: fragmento, escena, personaje, objeto, mapa o intervencion.
- `assetId` o ruta normalizada.
- Rol visual: imagen principal, puente, documento, formula, teatro visual, frame generado.
- Metadata de procedencia cuando exista.

Salida:

- Binding o campo de dominio con referencia estable al asset.
- Diagnostico de asset ausente, roto o incompleto.

Reglas:

1. `ProjectAssetCatalog` es la resolucion comun de assets.
2. `FragmentAssetBinding` gobierna relaciones por fragmento.
3. Teatro conserva imagenes de personaje/objeto/mapa porque su significado es teatral.
4. Visuales valida existencia y prepara proyecciones comunes; Teatro no debe duplicar esa validacion.
5. Ninguna modalidad debe inventar imagenes faltantes al exportar.

## Contrato: generacion visual

Owner futuro: capacidad Visuales; estado actual repartido entre `application/visual`, `application/modelsetup` y workflows teatrales.

Entrada minima:

- Prompt/contexto aprobado.
- Target semantico.
- Configuracion de motor local.
- Parametros de tamano/aspecto.
- Politica de reintentos y cancelacion.

Salida:

- Asset registrado o candidato visual.
- Estado observable de job.
- Error accionable si motor/modelo/recurso no esta listo.

Reglas:

1. Generacion IA es una operacion de Visuales, no el nombre de toda la capacidad.
2. Teatro puede construir contexto teatral, pero el motor/cola/readiness deben ser transversales.
3. Las tres modalidades deben poder usar Visuales sin duplicar motores.
4. Costos locales pesados no se disparan desde exportacion sin confirmacion.

## Contrato: readiness y exportacion

Owner actual: `InspectExportReadinessUseCase`, `ExportApplicationServices`, `ExportCenterCoordinator`.

Entrada minima:

- Proyecto y modo efectivo.
- Script/fragmentos.
- Storyboard/proyeccion visual cuando aplique.
- Jobs de audio.
- Proyeccion teatral cuando aplique.
- Archivo/carpeta de proyecto.

Salida:

- `ExportReadinessReport`.
- Items exportables, bloqueados o con advertencias.
- Evidencia y requisitos faltantes.

Reglas:

1. Cada modalidad presenta solo salidas validas para su modo.
2. Readiness vive en application; presentation lo muestra y despacha acciones.
3. Los faltantes deben ser accionables: que falta y donde resolverlo.
4. Exportacion no repara silenciosamente audio, imagenes, mapas ni estructura.
5. El Centro de exportacion es el punto transversal; no crear exportadores paralelos por workspace.

## Contrato: command/ribbon

Owner actual: `AppCommandId`, `AppCommandRegistry`, `CommandAvailabilityPolicy`, `RibbonDefinitionCatalog`.

Entrada minima:

- Comando estable.
- Modo efectivo.
- Estado del proyecto.
- Estado de seleccion o lectura cuando aplique.

Salida:

- Visible/oculto.
- Habilitado/deshabilitado.
- Razon de indisponibilidad.

Reglas:

1. El ribbon no decide comportamiento; despacha comandos.
2. La disponibilidad se basa en capabilities y estado real.
3. La redistribucion visual queda para Tanda 8.
4. Tanda 2 solo fija que `AppCommandId` es el contrato estable.

## Criterios para crear interfaces Java futuras

Crear una interfaz Java solo si se cumplen todos:

1. Existen dos o mas implementaciones concretas o una implementacion externa inevitable.
2. El consumidor esta en application y no puede depender de infrastructure.
3. El contrato no puede expresarse con un caso de uso existente.
4. Hay prueba que protege el limite.

Si no se cumplen, documentar el contrato y usar casos de uso actuales.
