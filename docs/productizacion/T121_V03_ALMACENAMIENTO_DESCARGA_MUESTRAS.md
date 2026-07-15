# T121-V03 — Almacenamiento seguro y descarga de muestras

## Objetivo

Esta tanda implementa la base de aplicación e infraestructura para que las muestras de voz puedan ser gestionadas de forma segura: importar, resolver su ruta gestionada, descargarlas/exportarlas a una carpeta elegida por el usuario y eliminarlas solo cuando el archivo pertenece a DocuPodcast.

## Decisión de producto

Las muestras de voz son propiedad sensible del usuario. La aplicación no debe borrar archivos externos originales ni escribir datos de usuario dentro de la carpeta de instalación. Las muestras gestionadas por el proyecto viven en:

```text
<proyecto>/voices/samples/
```

En tandas posteriores se ampliará a AppData para biblioteca global de voces. En esta tanda se consolida el caso de proyecto portable.

## Contrato implementado

### Importar muestra

`LocalVoiceSampleFileRepository.importSample(...)` sigue copiando la muestra seleccionada al proyecto y devuelve un `ProjectAssetReference` portable.

### Resolver muestra gestionada

Se agrega:

```java
resolveManagedSample(projectFile, sampleAsset)
```

Garantiza que el archivo resuelto permanece dentro de `voices/samples/` del proyecto.

### Descargar muestra

Se agrega:

```java
downloadSample(projectFile, sampleAsset, targetDirectory, preferredFileName)
```

Este método copia la muestra gestionada a una carpeta elegida por el usuario. No mueve ni borra el original gestionado. Si el nombre existe, genera un nombre único con sufijo.

### Eliminar muestra gestionada

Se agrega:

```java
deleteManagedSample(projectFile, sampleAsset)
```

Solo elimina archivos dentro de la carpeta gestionada `voices/samples/`.

## Use cases nuevos

### `DownloadVoiceReferenceSampleUseCase`

Permite descargar/exportar una muestra a una carpeta seleccionada por el usuario. La UI futura debe usar `DirectoryChooser` para obtener esa carpeta.

### `DeleteVoiceReferenceSampleUseCase`

Elimina solo archivos gestionados por DocuPodcast. Si la muestra tiene `VoiceFileOwnership.EXTERNAL_REFERENCE`, no borra el archivo externo y devuelve un mensaje claro.

## Reglas de seguridad

- No borrar archivos externos originales del usuario.
- No borrar recursos de instalación.
- No borrar voz neutral prediseñada protegida.
- Solo borrar archivos gestionados por DocuPodcast.
- Descargar muestra copia el archivo, no lo mueve.
- El usuario puede recuperar sus muestras con el botón Descargar.

## Relación con UX futura

En `T121-V08` la vista final de Voces debe mostrar por muestra:

```text
[Reproducir] [Reemplazar] [Descargar] [Eliminar]
```

El botón Descargar abrirá un `DirectoryChooser`. Esta tanda deja listo el backend de aplicación/infraestructura.

## Tests agregados

- `DownloadVoiceReferenceSampleUseCaseTest`
- `DeleteVoiceReferenceSampleUseCaseTest`
- `VoiceSampleStorageT121V03SourceTest`

## Criterios de aceptación

- La muestra se puede descargar a carpeta elegida sin borrar la original.
- La eliminación borra solo muestras gestionadas.
- Una muestra externa no se borra.
- Las rutas gestionadas quedan dentro de `voices/samples/`.
