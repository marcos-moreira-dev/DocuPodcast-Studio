# DOC-UX-HF9H — diálogo de importación ocultable y URLs de descarga editables

## Objetivo

Corregir dos problemas detectados en prueba manual:

1. La ventana secundaria de apertura documental podía quedar encima en estado “No responde” cuando el documento grande ya estaba visible detrás.
2. Las rutas de descarga de Voz IA avanzada y Voz local simple estaban demasiado incrustadas en código, sin punto editable para cambios futuros del proveedor.

## Cambios

- `DocumentImportProgressDialog` ahora tiene botón **Ocultar** y su `close()` desata bindings, oculta y cierra el diálogo.
- `DocumentWorkspaceView` limita el render inicial de documentos grandes a una ventana segura de bloques y muestra aviso de documento grande. La virtualización completa queda para `DOC-PERF-HF10`.
- `OperationalSettings.TtsEngineSettings` centraliza URLs de descarga con defaults, system properties y variables de entorno:
  - `DOCUPODCAST_XTTS_DOWNLOAD_BASE_URL`
  - `DOCUPODCAST_PIPER_RUNTIME_ZIP_URL`
  - `DOCUPODCAST_PIPER_DEFAULT_VOICE_URL`
  - `DOCUPODCAST_PIPER_DEFAULT_VOICE_METADATA_URL`
- `PropertiesOperationalSettingsRepository` persiste esas rutas en `operational-settings.properties`.
- `DownloadXttsOfficialModelUseCase` y `DownloadPiperPortableRuntimeUseCase` consumen las URLs configuradas.
- `SettingsDialog` expone campos editables de URL en la sección Motores de voz.

## Validación focal

- `javac --release 21` de settings/repositorio.
- `javac --release 21` de `application/modelsetup`.
- Source tests nuevos:
  - `DocUxHf9HImportDialogLargeDocumentSourceTest`
  - `EngineDownloadUrlCentralizationHf9HSourceTest`

## Límite consciente

Esta tanda no implementa virtualización completa del lector. Para documentos de miles de bloques, la vista queda protegida con render inicial acotado; la navegación virtual/paginada queda para `DOC-PERF-HF10`.
