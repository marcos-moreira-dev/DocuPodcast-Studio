# Tanda 58C — Smoke exploratorio mínimo

## Objetivo

Validar la base verde Tanda 58B desde la experiencia real de usuario antes de rediseñar scaffolding, migrar componentes GUI o refactorizar coordinadores.

Esta tanda no agrega funcionalidades productivas. Agrega protocolo, checklist, muestra DOCX, reporte manual y guardarraíl fuente para ejecutar una prueba exploratoria mínima.

## Principio de la tanda

Antes de tocar diseño o refactor fuerte, confirmar que el flujo mínimo existe y se entiende:

```text
crear/abrir proyecto
importar DOCX
ver documento
escuchar documento
generar guion/audio si falta
reproducir con buffer
guardar
cerrar
reabrir
volver a reproducir
```

El resultado de esta tanda debe clasificar los problemas encontrados en cuatro grupos:

```text
funcional: algo básico no opera;
UX: opera, pero se siente técnico o confuso;
arquitectura: opera, pero el código/estado queda frágil;
documentación: el comportamiento real no coincide con lo explicado.
```

## Archivos agregados

```text
docs/testeo/SMOKE_EXPLORATORIO_MINIMO_TANDA_58C.md
docs/testeo/checklists/58C_smoke_exploratorio_minimo.md
docs/testeo/reportes/REPORTE_SMOKE_EXPLORATORIO_TANDA_58C.md
samples/smoke/documento-simple-t58c.docx
scripts/17-smoke-exploratorio-minimo.bat
src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/SmokeExploratorioMinimoSourceTest.java
```

## Qué debe probar el usuario

1. Ejecutar tests para confirmar que la base sigue verde.
2. Abrir la app.
3. Crear un proyecto nuevo.
4. Importar `samples/smoke/documento-simple-t58c.docx`.
5. Confirmar que el documento se ve legible.
6. Usar la acción principal de escuchar/reproducir.
7. Confirmar si se crea guion automáticamente.
8. Confirmar si se genera audio con mock o motor local disponible.
9. Confirmar si el playback inicia con buffer.
10. Confirmar si el documento resalta el bloque/oración activa.
11. Guardar proyecto.
12. Cerrar la app.
13. Reabrir proyecto.
14. Confirmar que documento, guion, audio/manifest y estado básico siguen utilizables.

## Qué no debe evaluarse todavía

No convertir esta tanda en evaluación final de producto. Quedan fuera:

```text
rediseño completo de UI;
refactor de DocuPodcastShellViewModel;
MP4 final real;
configuración operativa completa de motores;
smoke largo con documentos complejos;
packaging app-image/MSI/RC.
```

## Resultado esperado

Al terminar la prueba manual, completar:

```text
docs/testeo/reportes/REPORTE_SMOKE_EXPLORATORIO_TANDA_58C.md
```

Si el flujo mínimo falla, la siguiente tanda debe corregir funcionalidad antes de rediseñar. Si funciona pero se siente técnico, la siguiente tanda debe pasar a criterios de diseño/scaffolding y componentes transversales.

## Próximo paso recomendado

```text
T59 — Criterios de diseño/scaffolding desde evidencia del smoke
T59A — Auditoría GUI/componentes transversales
T59B — Limpieza UX Documento
T60 — Refactor coordinadores
```
