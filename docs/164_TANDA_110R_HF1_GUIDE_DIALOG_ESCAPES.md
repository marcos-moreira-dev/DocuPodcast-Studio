# Tanda 110R-HF1 — Hotfix de compilación en GuideDialog

## Objetivo

Corregir el fallo de compilación detectado localmente en Windows después de aplicar T110R.

El diagnóstico `20260602-122634` fallaba en `mvn compile`, `mvn test` y smoke automático porque `GuideDialog.java` contenía literales Java mal escapados dentro del renderer de la guía.

## Fallo corregido

Errores reportados por Maven:

```text
GuideDialog.java:[148,67] illegal escape character
GuideDialog.java:[162,41] illegal line end in character literal
GuideDialog.java:[178,42] illegal escape character
GuideDialog.java:[178,45] illegal escape character
GuideDialog.java:[180,14] 'else' without 'if'
```

## Cambios

Se corrige `presentation/guide/GuideDialog.java`:

- `split("\R", -1)` pasa a `split("\\R", -1)`.
- El salto de línea del bloque de código queda como `append('\n')` en un literal Java válido.
- La expresión regular de listas numeradas queda escapada como `"^\\d+\\.\\s+.+"`.

## Alcance

Hotfix mínimo: no cambia contrato de producto, no cambia UI ni agrega funcionalidades. Solo restaura compilación de la reconstrucción T110R.

## Validación en entorno ChatGPT

- Inspección del diagnóstico local `20260602-122634`.
- Corrección focal de `GuideDialog.java`.
- `javac` focal sobre `GuideDialog.java` avanza hasta errores esperados de dependencias JavaFX no presentes en el entorno, sin reproducir errores de sintaxis Java.
- Escaneo fuente confirma que no quedan los literales mal escapados reportados.
- ZIP íntegro generado.

## Validación local requerida

Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

Resultado esperado: Maven compile vuelve a avanzar. Si aparecen fallos posteriores, ya no deberían ser los errores de `GuideDialog.java` corregidos en este hotfix.
