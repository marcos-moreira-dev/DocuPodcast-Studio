# MAVEN-DIAG-HF1 — tests fuente no leen artefactos binarios de motores

## Motivo

Tras descargar el modelo de Voz IA avanzada dentro de la raíz del proyecto, `mvn test` podía fallar en `NamespaceMigrationSourceTest` con un error de tamaño UTF-16 porque el test recorría todo el árbol del proyecto y trataba de leer como texto archivos binarios grandes como `models/tts/xtts/model.pth`.

## Decisión

Los tests de migración de namespace deben auditar código, documentación y configuración textual del repositorio, no artefactos descargados localmente por motores.

## Cambios

- `NamespaceMigrationSourceTest` ahora filtra explícitamente archivos textuales del proyecto.
- Excluye carpetas generadas/locales: `target`, `.git`, `models`, `tools`, `jobs`, `runtime` y `exports`.
- Excluye archivos sin extensión textual esperada.
- Excluye textos mayores a 2 MB para evitar que un reporte o artefacto accidental rompa Surefire.

## Alcance

No cambia producto visible, generación de audio ni Documento. Es un hotfix de diagnóstico para que Maven siga siendo confiable cuando el usuario ya descargó modelos grandes dentro del árbol de trabajo.

## Validación local ChatGPT

- Compilación focal del test con stubs JUnit: OK.
- Revisión fuente: el test ya no usa `Files.readString` sobre todo `Files.walk(.)` sin filtro.
