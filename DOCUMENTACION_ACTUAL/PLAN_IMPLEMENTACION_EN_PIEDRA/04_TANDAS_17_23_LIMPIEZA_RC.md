# Tandas 19–25 — limpieza, diagnóstico, packaging y RC

Este bloque convierte el proyecto en una RC personal verificable y reduce el ruido acumulado por la historia de tandas. La numeración se actualiza tras registrar el nuevo bloque de voces y sincronización con Documento.

---

## 19. TEST-CLEAN1 — limpieza y consolidación de tests

### Hallazgo de lectura

El proyecto tiene aproximadamente:

- 505 tests Java.
- 316 `SourceTest`.
- 204 tests en `productization/`.
- 94 tests que referencian Markdown.
- Muchos tests con `Files.readString(...)` y `contains(...)`.

### Objetivo

Separar tests vivos de arqueología de hotfixes.

### Categorías a crear

- Comportamiento real.
- Arquitectura.
- UI vigente.
- Documentación vigente.
- Source guardrails.
- Histórico archivado.

### Reglas

- No borrar guardarraíles críticos.
- Consolidar tests de playback acumulados.
- Evitar que tests viejos obliguen a conservar wording obsoleto.
- Mantener pruebas contra nombres técnicos en UI común.
- Mantener pruebas de arquitectura limpia.

### Criterio de salida

Los tests protegen producto vigente, no solo memoria histórica.

---

## 20. DOCS-CLEAN1 — documentación vigente mínima

### Hallazgo de lectura

Hay más de 1000 Markdown. `DOCUMENTACION_ACTUAL/` ya es la fuente vigente, pero README/AI_HANDOFF/VALIDATION siguen siendo cronologías acumuladas.

### Objetivo

Que un lector nuevo encuentre rápido la verdad vigente.

### Cambios

- README corto:
  - qué es;
  - cómo ejecutar;
  - cómo validar;
  - estado actual;
  - link a `DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md`.
- AI_HANDOFF corto:
  - estado técnico vigente;
  - riesgos;
  - próximas tandas;
  - qué no tocar.
- VALIDATION corto:
  - comandos;
  - smoke manual;
  - criterios RC.
- Docs históricos archivados o indexados como históricos.

### Regla de orden

No ejecutar `DOCS-CLEAN1` antes de `TEST-CLEAN1`, porque muchos tests todavía apuntan a Markdown histórico.

### Criterio de salida

La documentación vigente no marea ni contradice el plan actual.

---

## 21. PF9 — anti-placeholder global

### Hallazgo de lectura

La navegación principal ya está reducida a Inicio, Documento y Voces. Sin embargo, `WorkspaceViewRegistry` aún puede devolver `PlaceholderWorkspaceView` como fallback.

### Objetivo

Ningún botón visible del flujo normal debe terminar en placeholder.

### Cambios

- Workspace primario sin factory = error de desarrollo.
- Workspace heredado solicitado = redirigir o bloquear con motivo humano.
- `PlaceholderWorkspaceView` solo en tests o modo desarrollo explícito.
- Comandos visibles siempre tienen handler real o están deshabilitados.
- Soporte avanzado queda fuera del flujo común.

### Criterio de salida

No hay placeholders silenciosos en superficies de producto.

---

## 22. DIAGNOSTIC-SCRIPTS-RC1 — diagnóstico y gates reales

### Hallazgo de lectura

`99-diagnostico-completo.bat` es sólido, pero motores reales son opt-in. `16-release-candidate.bat` no exige necesariamente diagnóstico completo y algunos smokes son declarativos.

### Objetivo

Separar claramente diagnóstico base, motores reales, CUDA y RC.

### Cambios

- `16-release-candidate.bat` debe ejecutar o exigir `99-diagnostico-completo.bat`.
- Perfiles:
  - `RC-DEV`: sin motores reales.
  - `RC-LOCAL`: Voz local simple + FFmpeg.
  - `RC-FULL`: Voz IA avanzada CPU smoke real.
  - `RC-GPU`: CUDA smoke aprobado.
- Timeout de motores reales configurable.
- Smoke CUDA independiente.
- Reporte RC debe decir si motores reales fueron omitidos, aprobados o fallidos.

### Criterio de salida

Un RC no parece aprobado si los motores reales fueron omitidos.

---

## 23. PACKAGING-MEMORY-RC1 — heap en app-image/portable

### Hallazgo de lectura

`scripts/01-ejecutar-app.bat` usa `-Xmx2048m`, pero `jpackage`/app-image no necesariamente pasa el mismo heap.

### Objetivo

Que la app empaquetada tenga memoria suficiente para documentos grandes.

### Cambios

- Agregar `--java-options -Xmx2048m` o equivalente al app-image.
- Registrar heap en manifest portable.
- Mantener `DOCUPODCAST_APP_ROOT`.
- Verificar que portable usa rutas relativas.

### Criterio de salida

La app empaquetada no tiene menos memoria que el modo de ejecución local recomendado.

---

## 24. LEGAL-THIRD-PARTY-RC1 — terceros y redistribución

### Condición

Solo obligatoria si la app se distribuye fuera de la máquina/proyecto personal.

### Hallazgo de lectura

Ya existen manifests de terceros, pero algunos campos siguen como `Pendiente`.

### Objetivo

No distribuir binarios/modelos externos sin licencia/checksum claros.

### Revisar

- FFmpeg.
- Piper.
- Voces Piper.
- XTTS/Coqui.
- Modelos.
- Python portable.
- Dependencias Python.
- JavaFX/runtime.

### Estados permitidos

- Incluido con licencia/checksum.
- Descargado por el usuario.
- User-provided.
- Pendiente, pero entonces no distribuible públicamente.

### Criterio de salida

Para distribución pública, todo artefacto externo tiene licencia, origen y checksum.

---

## 25. RC-GATE1 — release candidate personal

### Objetivo

Cerrar una RC personal honesta.

### Gates obligatorios

- Maven compile/test verde.
- Diagnóstico completo verde.
- Voz local simple probada.
- Voz IA avanzada CPU probada o explícitamente marcada pendiente.
- GPU solo aprobada si CUDA smoke pasa.
- Audio final probado.
- Video MP4 probado.
- Guardar/reabrir probado.
- Sin placeholders visibles.
- Documentación vigente limpia.
- Tests limpios.
- Portable generado.
- Heap empaquetado configurado.

### Perfiles

- `RC-DEV`: útil para desarrollo, no promete motores reales.
- `RC-LOCAL`: recomendada para RC personal mínima.
- `RC-FULL`: incluye Voz IA avanzada CPU.
- `RC-GPU`: solo si GPU está confirmada por Python.

### Criterio de salida

Se puede entregar como RC personal sin ocultar pendientes.
