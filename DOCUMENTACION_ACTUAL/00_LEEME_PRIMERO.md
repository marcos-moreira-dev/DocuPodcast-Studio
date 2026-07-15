# Documentación actual de cierre — DocuPodcast Studio

Esta carpeta es la **fuente documental vigente** para cerrar DocuPodcast Studio. Se creó para no mezclar el estado actual del producto con la documentación histórica generada durante tandas anteriores.

## Autoridad documental vigente

`DOCUMENTACION_ACTUAL/` es la **única fuente de verdad operativa** para decidir próximas tandas, criterios de cierre y refactors. Las carpetas `docs/`, `DOCUMENTACION/`, `DOCUMENTACION_ESTRATEGICA/` y `00_MEMORIA_PROYECTO/` quedan como archivo histórico, trazabilidad y compatibilidad de pruebas; pueden consultarse para contexto, pero no deben gobernar decisiones nuevas si contradicen esta carpeta.

Regla práctica: si una decisión futura importa para producto, release candidate, arquitectura o diagnóstico, primero debe quedar resumida en `DOCUMENTACION_ACTUAL/`.

## Regla de lectura

1. Leer primero este archivo.
2. Leer `01_REGISTRO_TANDAS_RECIENTES.md` para conocer qué se ha implementado en la fase HF8/HF9/HF10G.
3. Leer `02_DECISIONES_PRODUCTO_CERRADAS.md` para no reabrir decisiones ya fijadas.
4. Leer `03_ROADMAP_PENDIENTE_CIERRE.md` para continuar con las tandas restantes.
5. Leer `13_MAPA_MAESTRO_CONSOLIDADO_TANDAS.md` para ubicar la puerta entre alineacion funcional y cierre RC.
6. Leer `08_ESTANDARES_PENDIENTES_RC.md` para conocer los estándares pendientes reales de cierre, con criterios de implementación y aceptación.
7. Para el bloque de voces, leer `PLAN_IMPLEMENTACION_EN_PIEDRA/07_VOCES_VISTA_Y_WIZARD_REFERENCIAS.md`.
8. Leer `10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md` antes de iniciar refactors grandes o limpieza documental.
9. Leer `11_REFERENCIA_TECNICA_ARQUITECTURA_LOCAL.md` para una referencia tecnica local de capas, audio, chunks, imagenes y procesos.
10. Leer `12_GENERACION_AUDIO_IMAGEN_MICROFONOS.md` para las decisiones vigentes de reintentos, pausas, microfonos, grabacion manual y aspecto de imagen.
11. Leer `04_VALIDACION_Y_SMOKE.md` antes de entregar cualquier ZIP.

## Plan de implementación en piedra

Tras la lectura masiva final, el plan operativo vigente quedó documentado en:

`DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/00_INDICE.md`

Ese plan fija el orden de implementación hacia RC personal y debe prevalecer sobre roadmaps históricos salvo que un diagnóstico local nuevo demuestre una falla bloqueante distinta.

El mapa consolidado `13_MAPA_MAESTRO_CONSOLIDADO_TANDAS.md` fija la secuencia completa de Tandas 4-16 y declara que la alineacion con Word/respaldo termina al cerrar la Tanda 10.

## Estado general

La aplicación ya está en fase de cierre funcional. El foco actual no es ampliar alcance, sino estabilizar y pulir:

- Documento como lector limpio con selección por frase.
- Rail Visual derecho virtualizado con una tarjeta lógica por frase/fragmento.
- Capas visuales guardadas en el proyecto, no en el Word original.
- Playback por chunks con duración real de WAV.
- Preparación de motores de voz con lenguaje humano.
- Vista Voces ya pasó por `VOICE-UX-POLISH1A`: administra voces y muestras de referencia sin dashboard decorativo; quedan pendientes el wizard de grabación Java y la sincronización transversal con Documento.
- Exportación de audio/video pendiente de cierre final.
- Refactor transversal pendiente antes del release candidate.
- Base vigente: DOC-INDEX-HF13 sobre EXPORT-CLEAN1 verde; Vista Voces expone catálogo teatral extendido, Documento/render usan voces/tonos reales, Configuración distingue descarga/modelo/runtime de Voz IA avanzada y las exportaciones técnicas quedan en soporte avanzado y Documento incluye un índice lateral plegable por títulos/secciones.

## Documentación histórica

Las carpetas antiguas `docs/`, `DOCUMENTACION/`, `DOCUMENTACION_ESTRATEGICA/` y `00_MEMORIA_PROYECTO/` **se conservan por compatibilidad de pruebas, trazabilidad y memoria histórica**. No deben tomarse como el punto de entrada principal para decidir próximas tandas.

Punto de entrada vigente: `DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md`.

## Nota de continuidad RC — ESTANDARES-PENDIENTES-RC1

La base local validada por el usuario tiene diagnóstico completo OK y demo teatral estable. Para continuar sin depender del contexto de chat, leer obligatoriamente `08_ESTANDARES_PENDIENTES_RC.md` y `PLAN_IMPLEMENTACION_EN_PIEDRA/08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md`. Estos archivos documentan estándares pendientes, orden recomendado, criterios de aceptación y guardarraíles de UI operativa, rutas runtime, procesos externos, motores, persistencia y RC.
