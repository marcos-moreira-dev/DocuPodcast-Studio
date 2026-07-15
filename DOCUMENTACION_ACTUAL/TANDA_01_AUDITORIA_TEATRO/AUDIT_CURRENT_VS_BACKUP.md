# Tanda 1 - Auditoria Current vs Backup

Fecha de auditoria: 2026-06-24

## Alcance

Esta auditoria compara el arbol actual de trabajo:

`C:\Users\MARCOS MOREIRA\Downloads\g`

contra el respaldo de estudio, tratado como solo lectura:

`C:\Users\MARCOS MOREIRA\Downloads\docupodcast studio respaldo estudiar`

El alcance de esta tanda es teatro y dependencias directas: gramatica, persistencia JSON, UI teatral, workflows de shell, video teatral y exportacion. No se migro codigo, no se copiaron clases del respaldo y no se modificaron APIs, tipos publicos, logica productiva ni persistencia.

## Baseline de entorno

| Item | G actual | Respaldo |
| --- | --- | --- |
| Ruta | `C:\Users\MARCOS MOREIRA\Downloads\g` | `C:\Users\MARCOS MOREIRA\Downloads\docupodcast studio respaldo estudiar` |
| Git usable | No. `git status` falla con `fatal: not a git repository (or any of the parent directories): .git` aunque existe una carpeta `.git` vacia o rota. | No. No hay `.git`. |
| Regla de escritura | Carpeta de trabajo permitida. | Solo lectura para esta tanda. |
| Java main | 964 archivos | 873 archivos |
| Java test | 686 archivos | 656 archivos |
| Resources | 146 archivos | 146 archivos |

`DOCUMENTACION_ACTUAL/` se mantiene como fuente operativa vigente. Directorios historicos como `docs/`, `DOCUMENTACION/`, `DOCUMENTACION_ESTRATEGICA/` y `00_MEMORIA_PROYECTO/` no reciben decisiones nuevas en esta tanda.

## Metodo de comparacion

Filtro usado sobre `src`:

`theatre|teatro|grammar|spatial|intervencion|theater|export`

Cada archivo filtrado se comparo por ruta relativa y SHA-256. Los hashes de conjunto permiten repetir la auditoria sin depender de Git.

| Conjunto | Conteo | SHA-256 de conjunto |
| --- | ---: | --- |
| Relevantes en G | 237 | `b63eaf7523afb828a3f89f0f01a2c55bc1a28320120ed7db8b1269b94b81d382` |
| Relevantes en respaldo | 200 | `34f483483c58a00cec523d39f3b04f04a3e8f26a10854ddad2084ea28e3f8136` |
| Mismas rutas y mismo hash | 184 | `93011a0e95ec73dfc3b3de7561d8448c9e5cc4d5b56c0839333f1b5a35c9e9ab` |
| Mismas rutas con hash distinto | 16 | `0aa9bdcf274e6e939073fa20e641fca7f9383ab2903e18c32c04521c905cb7fa` |
| Solo en G | 37 | `22e094a727c2ab14c39cb80302340b379da69271140dcb99d234c20cc6dfc3a7` |
| Solo en respaldo | 0 | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |

Interpretacion inicial: G es un superconjunto funcional del respaldo en los archivos filtrados. No hay piezas relevantes de teatro/exportacion que existan solo en el respaldo. Por tanto, el respaldo sirve como linea base de contraste, no como fuente para copiar de vuelta.

## Conteo por capas

| Capa | G | Respaldo | Lectura |
| --- | ---: | ---: | --- |
| `domain/theatre` | 1 | 1 | Nucleo teatral estable e igual. |
| `domain/theatre/plan` | 6 | 6 | Planes teatrales estables. |
| `application/theatre` | 26 | 19 | G agrego proyeccion, readiness y enlaces de fragmento. |
| `application/theatre/grammar` | 2 | 0 | G movio gramatica teatral a application. |
| `application/grammar` | 8 | 0 | G agrego capa transversal de gramatica. |
| `infrastructure/json` | 5 | 5 | Persistencia base presente en ambos. |
| `infrastructure/grammar` | 1 | 0 | G agrego repositorio de semantica. |
| `presentation/theatre` | 35 | 35 | Mismo volumen; parser/template cambiaron. |
| `presentation/shell/workflow` | 54 | 53 | G agrego coordinacion de gramatica. |
| `application/video` | 31 | 31 | Mismo volumen; plan de video teatral cambio. |
| `application/export` | 23 | 23 | Mismo volumen; readiness/exportables cambiaron. |
| `presentation/export` | 6 | 0 | G agrego Centro de exportacion. |

## Archivos iguales clave

Los 184 archivos iguales confirman que el respaldo conserva buena parte del nucleo comun. Los anclajes mas importantes para teatro son:

| Archivo | Decision de lectura |
| --- | --- |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/theatre/TheatreProjectLayer.java` | Igual en ambos. Mantiene el contrato de datos teatral. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/theatre/plan/*` | Igual en ambos. Mantiene planes/estructuras de dominio ya aceptadas. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonReader.java` | Presente en ambos y cubre lectura teatral. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonWriter.java` | Presente en ambos y cubre escritura teatral. |

`TheatreProjectLayer` conserva el mismo nucleo: intervenciones, personajes, alias de voz, imagenes de personaje, visuales de intervencion, actos, escenas, posiciones, acciones, placements de texto, objetos e imagenes de objetos.

## Archivos cambiados

| Archivo | SHA-256 G | SHA-256 respaldo | Lectura para migracion |
| --- | --- | --- | --- |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportableArtifactKind.java` | `279a97ce2b593206b53b01d645526fba08af34a82b7788e5ca286e03faebdc8a` | `6360801a4b63df19e497839f169dbfcc45deac3343a01da9e73f7ec279c318ef` | G amplio los tipos exportables/readiness. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportDiagnosticReportUseCase.java` | `ed38ce77e3ea72a16e6d1f2b87f6df16d21f40661245db7c975b059bd322c623` | `f964a6c6af9fd5e6a7cf4ec1f7744f0ae0d735cb8ca15da1a7a2127b541af855` | Diagnostico exportable cambio junto al modelo actual. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCase.java` | `01e4e2b66729f93a42faa74305eccadc843e96e98b792f331975c38652b8c2e4` | `9709a56b018517b2b7ead4acf4380eb02ef19f7fb9e3c0ab142c732b16cacf21` | G incluye readiness teatral por obra/mapa/porcion. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildTheatreSpatialVideoPlanUseCase.java` | `e996d7b0b51b2c437d762df5202fc8d3de319a47c3c3c29c4f7e6588ce35af1f` | `098ecedd6832d59f536accf7c1b564a1b424eac7667cdf0a557db8d743733caf` | G tiene logica actual de mapa espacial teatral. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/ExportFinalVideoUseCase.java` | `1d792085b55fba4d10a1e2704d80e345af8dd70818dd347ea0059b1e9e0962b0` | `1443eace3b236eea3546febdb44609256e9a0ae0139bcbd1f36fc3d1264bcfed` | Export final cambio junto a salidas actuales. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/export/FileSystemProjectBundleExporter.java` | `92f88bb68d3ccfca4bc5e3e14b76bea877056b4e6b07d58ab9bf7a45ea028e14` | `df0d3fad2e43a8022661ff68ee6c806887d45dbde69eb91dcfdb6a8e3ab5df47` | Bundle tecnico cambio; no recuperar desde respaldo sin revisar. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/IncompleteAudioExportDialog.java` | `4362f1909d7d3b05a27821125210bc9dc9cc0f3a5a3fca50b93c5f583c96c17e` | `d861d58070baf78cd93d57d7a89400976564805b61a890c82d177aa785e62267` | Mensajeria de exportacion/audio cambio. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java` | `32829859a68fd5193ddc60529b51ba7928ce2514aa30cdff81a0853bfa8e9758` | `cdd7e92ae93512735e6f95bfb1780832c78215048b25127a0be68c2e82f57710` | G contiene coordinacion actual de exportaciones teatrales. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreGrammarMarkdownParser.java` | `20eedc1f41139fff1f8f28976a27202320660a318e0f89a1ad61be849d12521f` | `f87274eb2f23d54eb3265d84eee071980953d479740ad038d3d8216042584812` | Parser de presentation ya no es la fuente principal. |
| `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreGrammarTemplate.java` | `c165c85e60417958b434bf1dc2af27827a03774e9b50c4c0ec94ab58b0e4df35` | `d680db5dceb2d4d526af9acd46a8119394f6055b0eca8282584b7b6ff608f27c` | Template de presentation quedo como superficie compatible. |
| `src/test/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCaseTest.java` | `8f2ba1ff9fd18c62884e25293a48fe5c54089f6f08194b2756f011524b2bf3ca` | `45025ba9e2e36f5deddc115ffcd5e77aef9d3000f7d1926cf03296491af0dddf` | Pruebas de readiness se ajustaron a G. |
| `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportUiSourceTest.java` | `ebafa96b181a87cdd0d2b3e5181f388a39cb1a1d7a3e4652a3092501580b4797` | `d7e28e6318b1b44ff9be541d339ee0aca80d1c625925584286836feef5debb2f` | UI/export tiene expectativa actual distinta. |
| `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreFrameGenerationWorkflowTest.java` | `bf58189ee9998037edfd5356b0a6e5870f52c251e7c557ca23f470a390163694` | `2e899dce1d6efdc997f51fd6dd43e3dcdd38b935cf7c0d7f9f88157466f233af` | Imagen/frame teatral cambio. |
| `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreImageGenerationWorkflowTest.java` | `cbae2c27c0b7bd6c44e4e4dd87d659b26a719397fd6bcc7c929f775bd1d2ce92` | `485a3e84fa8e2e2e7ac6656977cd0dc6149ddc02e1a5950db6c173863a45d5f2` | Imagen teatral tiene reglas actuales. |
| `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreGrammarRibbonSourceTest.java` | `e10d45f5073b3abf3ed716459e1167934f797ea578c5f7099b049e5dce87978e` | `dfc018316e2f19f6764a9f939ff8a60f6607f737035fef1f029219123e3f3557` | Ribbon/gramatica teatral cambio en G. |
| `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/TheaterColonRead1SourceTest.java` | `dc040c9da3933dfffc754bf080c9bdff581ff311b3213e9783f5397653a41c1c` | `1bf14321c48f5e5cb0f485cd152c7bda7319efc11ae8bea416d93341c02b108e` | Regla de producto sobre lectura teatral cambio. |

## Archivos solo en G

| Grupo | Archivos |
| --- | --- |
| Gramatica transversal | `BuildGrammarTemplateUseCase`, `GrammarDiagnostic`, `GrammarDiagnosticSeverity`, `GrammarImportReport`, `ImportProjectGrammarMarkdownUseCase`, `ProjectGrammarKind`, `ProjectSemanticsDocument`, `ProjectSemanticsRepository` |
| Gramatica narrativa | `NarrativeVideoGrammarMarkdownParser`, `NarrativeVideoGrammarTemplate` |
| Servicios de aplicacion | `GrammarApplicationServices`, `TheatreApplicationServices` |
| Teatro application | `BuildTheatreProductionProjectionUseCase`, `TheatreFragmentLink`, `TheatreFragmentLinkPolicy`, `TheatreProductionIntervention`, `TheatreProductionProjection`, `TheatreProductionReadiness`, `TheatreProductionScene` |
| Teatro application grammar | `application/theatre/grammar/TheatreGrammarMarkdownParser`, `application/theatre/grammar/TheatreGrammarTemplate` |
| Infraestructura semantica | `FileSystemProjectSemanticsRepository` |
| Centro de exportacion | `ExportCenterAction`, `ExportCenterCoordinator`, `ExportCenterDialog`, `ExportCenterSelection`, `ExportCenterState`, `ExportTargetPresentation` |
| Shell workflow | `GrammarWorkflowCoordinator` |
| Pruebas nuevas | `ImportProjectGrammarMarkdownUseCaseTest`, `NarrativeVideoGrammarMarkdownParserTest`, `BuildTheatreProductionProjectionUseCaseTest`, `ExportCenterCoordinatorProcessJobsTest`, `ExportCenterCoordinatorTest`, `CorrectiveRibbonFragmentExportSourceTest`, `MarkdownGrammarTanda8SourceTest`, `Megatanda5ExportCenterSourceTest` |

No hay archivos solo en respaldo dentro del filtro de teatro/gramatica/exportacion. Esto descarta, para Tanda 1, una recuperacion directa de codigo teatral perdido desde el respaldo.

## Lectura por comportamiento

1. El dominio teatral no se debe sustituir: `TheatreProjectLayer` ya coincide entre G y respaldo.
2. La persistencia JSON teatral ya existe en ambos lados y G conserva el contrato de lectura/escritura.
3. La diferencia real esta en capas superiores: gramatica de application, materializacion semantica, readiness teatral, Centro de exportacion y coordinacion del ribbon.
4. Los cambios en exportacion y video no son ruido: impactan obra completa, mapa teatral y porcion de obra.
5. Las clases nuevas de G deben ser tratadas como trabajo vigente, no como deuda a revertir.

## Validacion ejecutada

No se ejecutaron builds ni tests en el respaldo. En G se ejecuto:

```powershell
mvn -q "-Dtest=DocuPodcastProjectTheatreJsonTest,TheatreGrammarMarkdownParserTest,BuildTheatreProductionProjectionUseCaseTest,BuildTheatreSpatialVideoPlanUseCaseTest,InspectExportReadinessUseCaseTest" test
```

Resultado: PASS, exit code 0, 23.9 s.

| Prueba | Resultado | Razon de inclusion |
| --- | --- | --- |
| `DocuPodcastProjectTheatreJsonTest` | PASS | Roundtrip JSON de la capa teatral. |
| `TheatreGrammarMarkdownParserTest` | PASS | Parsing de gramatica teatral y plantilla. |
| `BuildTheatreProductionProjectionUseCaseTest` | PASS | Proyeccion/readiness de produccion teatral. |
| `BuildTheatreSpatialVideoPlanUseCaseTest` | PASS | Plan de video del mapa espacial teatral. |
| `InspectExportReadinessUseCaseTest` | PASS | Readiness de salidas exportables, incluidas teatrales. |

## Estado de archivos de la tanda

Archivos documentales nuevos esperados en esta tanda:

1. `DOCUMENTACION_ACTUAL/TANDA_01_AUDITORIA_TEATRO/AUDIT_CURRENT_VS_BACKUP.md`
2. `DOCUMENTACION_ACTUAL/TANDA_01_AUDITORIA_TEATRO/THEATER_CAPABILITY_MAP.md`
3. `DOCUMENTACION_ACTUAL/TANDA_01_AUDITORIA_TEATRO/THEATER_GAP_ANALYSIS.md`

No se tocaron fuentes `src/main`, fuentes `src/test`, recursos productivos ni el respaldo.
