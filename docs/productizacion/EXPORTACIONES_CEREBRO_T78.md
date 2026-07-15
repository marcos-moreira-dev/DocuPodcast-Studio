# Exportaciones del cerebro — T78

T78 centraliza el criterio de exportación en `InspectExportReadinessUseCase`.

La interfaz futura no debe decidir por su cuenta si una salida está disponible. Debe consultar el cerebro o delegar en los casos de uso ya existentes.

## Matriz de preparación

Cada salida tiene:

- tipo estable (`ExportableArtifactKind`),
- formato (`DocuPodcastExportFormat`),
- estado (`EXPORTABLE`, `EXPORTABLE_CON_ADVERTENCIAS`, `BLOQUEADO`),
- evidencia,
- faltantes,
- limitaciones honestas.

## Reporte auditable

El paquete completo ahora escribe:

```text
reports/EXPORT_READINESS.md
```

Ese archivo explica qué puede salir, qué falta y qué no se promete. Sirve para smoke automático, soporte y release candidate.

## Principio

Exportar no debe ser una ilusión visual. Si el usuario ve una opción, debe existir una cadena real o un reporte claro de faltantes.
