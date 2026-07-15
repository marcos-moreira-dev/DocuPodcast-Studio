# Plan de implementación en piedra — índice

Esta carpeta conserva el cierre de lectura masiva y el plan de implementación acordado para llevar DocuPodcast Studio a una **RC personal**.

Este material se creó porque las conclusiones de la lectura masiva son demasiado importantes para quedar solo en una conversación. La documentación histórica del proyecto sigue existiendo por trazabilidad, pero este plan es la referencia operativa vigente para continuar.

## Regla de autoridad

1. Para decidir próximas tandas, leer primero este índice.
2. Luego leer `01_PLAN_MAESTRO_IMPLEMENTACION.md`.
3. Usar los archivos `02`, `03` y `04` para implementar por bloques.
4. Usar `05_MATRIZ_DEPENDENCIAS_Y_GATES.md` antes de cambiar el orden.
5. Usar `06_CIERRE_LECTURAS_MASIVAS.md` para recordar por qué se fijó este plan.

Si un roadmap histórico contradice este plan, este plan tiene prioridad, salvo que un diagnóstico local nuevo demuestre una falla bloqueante distinta.

Para ver el mapa completo que une el Word maestro, las Tandas 1-3 ya cerradas, las Tandas 4-10 de alineacion funcional y las Tandas 11-16 de cierre RC, leer `../13_MAPA_MAESTRO_CONSOLIDADO_TANDAS.md`.

## Archivos

- `01_PLAN_MAESTRO_IMPLEMENTACION.md` — orden inamovible actualizado de 25 tandas.
- `02_TANDAS_01_09_FUNCIONALES_UX.md` — bloque funcional y UX principal; el contenido actualizado cubre tandas 01–11 tras dividir Voces.
- `03_TANDAS_10_16_REFACTOR_EXPORT_RC.md` — refactor transversal, audio/video y persistencia; el contenido actualizado cubre tandas 12–18.
- `04_TANDAS_17_23_LIMPIEZA_RC.md` — limpieza, diagnóstico, packaging, legal y RC; el contenido actualizado cubre tandas 19–25.
- `05_MATRIZ_DEPENDENCIAS_Y_GATES.md` — dependencias, gates y reglas de no adelantar tandas.
- `06_CIERRE_LECTURAS_MASIVAS.md` — síntesis final de lecturas.
- `07_VOCES_VISTA_Y_WIZARD_REFERENCIAS.md` — contrato operativo de Vista Voces, muestras de referencia, wizard de grabación y sincronización con Documento.
- `08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md` — mapa de continuidad RC con estándares pendientes reales, dependencias, orden recomendado y reglas que no deben romperse.

## Tesis del plan

El producto ya no necesita más expansión de alcance. Necesita cerrar estos frentes:

- Voz IA avanzada descargada, verificada y realmente usable.
- CPU/GPU sin promesas falsas.
- Reproducción desde índice/selección y continuidad a velocidad alta.
- UX puntual de Voces y Documento.
- Exportaciones audio/video sin congelar ni exponer paquetes técnicos.
- Refactor transversal de rutas, mensajes, procesos y descargas.
- Tests y documentación orientados a producto vigente, no a arqueología de tandas.
- Diagnóstico y RC con evidencia real.

## Próxima tanda obligatoria

La próxima implementación debe ser:

**VOICE-UX-POLISH1A — implementada: limpieza operativa de Vista Voces.**

Las tandas 1 a 4 ya están implementadas y validadas localmente por el usuario. Antes de construir el wizard de grabación, se debe limpiar la Vista Voces actual: quitar elementos sin función, corregir microcopy, simplificar labels y dejar claro que la vista sirve para administrar voces y muestras de referencia que luego usa Documento.

## Nota ESTÁNDARES-PENDIENTES-RC1

Después de estabilizar el demo teatral y validar diagnóstico completo OK, el plan de cierre debe continuar por estándares transversales. La próxima tanda recomendada es `RUNTIME-PATHS-RF1`, seguida de `EXTERNAL-PROCESS-RUNNER-RF1`. El detalle exhaustivo está en `08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md` y `../08_ESTANDARES_PENDIENTES_RC.md`.
