# Contrato de carpeta contenedora — T80B

## Regla

Un proyecto DocuPodcast V1 es una carpeta contenedora. Dentro de esa carpeta viven:

```text
<Proyecto>/
  <Proyecto>.docupodcast.json
  source/
  document/
  script/
  storyboard/
  assets/
  jobs/
  reports/
  exports/
```

No se debe guardar `source/`, `document/`, `script/`, `storyboard/`, `jobs/` o assets directamente en el Escritorio solo porque el usuario eligió `Escritorio/<Proyecto>.docupodcast.json`.

## Política de ruta

`ProjectContainerPathPolicy.resolveSaveAsTarget(...)` traduce selecciones de Save As:

```text
Parent/Nombre.docupodcast.json -> Parent/Nombre/Nombre.docupodcast.json
Parent/Nombre                  -> Parent/Nombre/Nombre.docupodcast.json
Parent/Nombre/Nombre.docupodcast.json -> se conserva
```

## Compatibilidad

La app mantiene apertura de proyectos existentes. La política se aplica a proyectos nuevos o guardados con **Guardar como**.

## Relación con próximas tandas

Esta tanda se adelanta a la entrada flexible de media porque MP3, WAV, imágenes y video→audio aumentan el número de recursos físicos. Sin carpeta contenedora, esos recursos se vuelven incómodos de manejar y reparar.
