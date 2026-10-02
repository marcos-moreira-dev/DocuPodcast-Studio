# Publicación del código — 2026-10-01

La actualización se prepara en `codex/publish-studio-update`, conservando el historial de `main`.

## Contenido

Se incluyen fuentes, pruebas, scripts, documentación, manifiestos, workflows, interfaz y recursos oficiales de ejemplo. Las muestras de voz ya versionadas se conservan.

Se excluyen pesos de IA, motores instalados, entornos Python, distribuciones, diagnósticos, estado local, exportaciones y reglas MathCAT extraídas del paquete Maven. No se eliminan esos archivos del equipo.

## Correcciones de validación — 2026-10-02

Se separaron los valores persistentes de video/exportación del nivel de aplicación; las ventanas de lotes y los casos de uso de paquetes teatrales reciben sus dependencias mediante puertos desde bootstrap. Se reutilizan las fábricas de controles y diálogos compartidos y se evita que la presentación construya adaptadores o codifique imágenes.

También se corrigieron la detección de JSON teatral corrupto y un icono semántico ausente. Las expectativas desactualizadas de nombres, permisos de modelos, geometría de interfaz, OCR y transparencia se alinearon con sus contratos actuales. El auditor UTF-8 ahora distingue correctamente un lector con charset explícito y una llamada anidada, con una prueba de regresión.

## Validación

Verificación completa del reactor y de la interfaz terminada correctamente el 2026-10-02:

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=D:/Proyectos/g/runtime/ipc'
mvn -q verify -Pgui-e2e
```

La propiedad usa una ruta IPC corta para las pruebas de red en Windows; debe adaptarse a la ubicación del checkout.

| Suite | Pruebas | Fallos | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: |
| Media API | 11 | 0 | 0 | 0 |
| Tinta | 55 | 0 | 0 | 0 |
| Adaptadores locales | 135 | 0 | 0 | 13 |
| Escritorio | 1583 | 0 | 0 | 16 |
| Interfaz E2E (headless) | 13 | 0 | 0 | 0 |
| Lanzador | 7 | 0 | 0 | 0 |
| **Total** | **1804** | **0** | **0** | **29** |

Los informes corresponden a esta ejecución, sin contar resultados antiguos. Los fallos de la preparación inicial quedaron resueltos. Las pruebas condicionales omitidas no se consideran aprobadas.

No se ejecutaron generaciones completas con modelos reales ni el renderizado completo del ensayo para esta publicación. La validación automatizada no equivale a certificar cada combinación de hardware y motores.

La actualización permanece en el PR #1 para revisión; no se ha fusionado a `main` ni publicado una release estable.
