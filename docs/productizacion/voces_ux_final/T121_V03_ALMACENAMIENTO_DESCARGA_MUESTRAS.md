# T121-V03 — Almacenamiento seguro y descarga de muestras

## Objetivo

Gestionar archivos de muestra de voz de forma segura: grabar, importar, reemplazar, descargar/exportar y eliminar sin tocar archivos externos no controlados por DocuPodcast.

## Carpetas

### AppData

```text
<AppData>/DocuPodcast Studio/voices/
```

Uso:

- voces globales del usuario;
- muestras reutilizables;
- cache controlada.

### Proyecto

```text
<proyecto>/assets/voices/
```

Uso:

- voces específicas del proyecto;
- muestras asociadas al documento;
- portabilidad al enviar ZIP del proyecto.

### Instalación

```text
<install-dir>/
```

Uso:

- recursos base;
- voz neutral prediseñada protegida.

No se deben escribir ni borrar datos de usuario aquí.

## Acciones por muestra

- Grabar.
- Importar.
- Reproducir.
- Reemplazar.
- Descargar/exportar.
- Eliminar.

## Descargar muestra

La acción `Descargar muestra` debe:

1. Abrir `DirectoryChooser`.
2. Permitir elegir carpeta.
3. Copiar el archivo de muestra.
4. Mantener nombre claro:
   - `maria-neutral.wav`
   - `maria-heroica.wav`
5. Mostrar confirmación.
6. No mover ni borrar el archivo original gestionado.

## Eliminar muestra

Reglas:

- Si es APP_RESOURCE protegido, no eliminar.
- Si es USER_APPDATA o PROJECT_ASSET, eliminar con confirmación.
- Si es EXTERNAL_REFERENCE, solo eliminar referencia.
- Si la muestra eliminada era neutral y la voz avanzada queda sin neutral, marcar voz como incompleta.

## Use cases

- `ImportVoiceReferenceSampleUseCase`
- `RecordVoiceReferenceSampleUseCase`
- `ReplaceVoiceReferenceSampleUseCase`
- `DownloadVoiceReferenceSampleUseCase`
- `DeleteVoiceReferenceSampleUseCase`
- `PlayVoiceReferenceSampleUseCase`
- `InspectVoiceSampleOwnershipUseCase`

## Diálogos

### Confirmar eliminación

```text
Eliminar muestra “María — Feliz”

Se eliminará la copia gestionada por DocuPodcast.
Esta acción no borrará archivos externos originales.

[Cancelar] [Eliminar muestra]
```

### Descargar muestra

```text
Muestra descargada correctamente.
Archivo: maria-feliz.wav
```

## Tests recomendados

- `DownloadVoiceReferenceSampleUseCaseTest`
- `DeleteVoiceReferenceSampleDoesNotDeleteExternalOriginalTest`
- `ProtectedDefaultSampleCannotBeDeletedTest`
- `VoiceSampleDirectoryChooserSourceTest`
- `VoiceSampleOwnershipPolicyTest`

## Criterios de aceptación

- Toda muestra puede descargarse.
- No se borran archivos externos.
- La voz neutral prediseñada está protegida.
- El usuario puede recuperar sus muestras.
