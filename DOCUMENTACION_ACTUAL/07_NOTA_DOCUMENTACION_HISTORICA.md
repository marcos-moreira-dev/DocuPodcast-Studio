# Nota sobre documentación histórica

No se elimina documentación histórica en esta tanda porque existen guardarraíles y pruebas que todavía verifican rutas antiguas. Borrarla ahora aumentaría el riesgo de romper el diagnóstico completo.

La regla operativa queda así:

- `DOCUMENTACION_ACTUAL/` = fuente vigente de cierre.
- `docs/`, `DOCUMENTACION/`, `DOCUMENTACION_ESTRATEGICA/`, `00_MEMORIA_PROYECTO/` = memoria histórica/compatibilidad.
- Nuevas decisiones de cierre deben registrarse primero en `DOCUMENTACION_ACTUAL/`.
