# Contrato del cerebro para refresco de documento fuente

## Decisión

`Refrescar contenido` es una capacidad del cerebro de la app, no una acción visual aislada.

La interfaz podrá mostrar un botón simple, pero ese botón debe delegar en una cadena de aplicación:

```text
UI → SourceDocumentRefreshCoordinator → RefreshSourceDocumentUseCase → ImportDocumentUseCase → SourceDocumentChangeReport
```

## Responsabilidades

### UI

- mostrar la acción `Refrescar contenido` cuando exista un documento fuente;
- mostrar resumen humano del reporte;
- pedir confirmación si hay cambios que vuelven obsoleto el audio;
- no decidir por sí sola qué borrar o regenerar.

### Coordinador

- tomar el documento narrable activo;
- llamar el caso de uso;
- actualizar estado de sesión/proyecto cuando corresponda;
- marcar artefactos derivados como vigentes, obsoletos o en revisión;
- mantener el flujo en Documento, sin llevar al usuario a cabina técnica.

### Caso de uso

- reimportar fuente;
- comparar snapshot previo y actual;
- producir `SourceDocumentChangeReport`;
- no modificar el archivo fuente.

### Dominio

- expresar snapshot, estado de cambio y vigencia de artefactos derivados;
- no depender de JavaFX;
- no depender de infraestructura.

## Estados mínimos

```text
UNCHANGED → audio/capas/storyboard vigentes
CHANGED → audio obsoleto, capas/storyboard en revisión
MISSING → fuente faltante, revisión manual
UNSUPPORTED → refresco no disponible para esa fuente
```

## Límites V1

V1 no necesita resolver automáticamente todos los conflictos de rangos. Debe informar, conservar trabajo del usuario y marcar revisión. La reconciliación fina de rangos puede venir después.
