# Memoria — Tanda 15 implementada

La Tanda 15 cierra el ciclo de MVP técnico con scripts de release candidate. Se añadieron scripts públicos para revalidación local completa, app-image, MSI, release candidate y JavaDoc.

La corrección importante derivada de pruebas locales del usuario fue que los scripts deben poder ejecutarse desde `scripts\` o desde la raíz. Esa regla se mantiene en todos los scripts nuevos.

También se actualizó `02-ejecutar-tests.bat` para que no parezca silencioso cuando los tests pasan: ahora imprime `Tests OK`.

El producto todavía requiere una tanda posterior de polish visual. La Tanda 15 prioriza cierre funcional, validación, packaging y handoff.
