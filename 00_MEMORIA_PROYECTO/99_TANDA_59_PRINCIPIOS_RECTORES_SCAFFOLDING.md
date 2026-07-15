# Memoria — Tanda 59 — Principios rectores de rediseño y scaffolding

La Tanda 59 fija la brújula para rediseñar DocuPodcast después de confirmar base verde y antes de refactorizar de forma profunda.

## Decisiones preservadas

- Documento es pantalla principal.
- Guion, Audio, Storyboard, Voces, Configuración y Diagnóstico son superficies de apoyo.
- La complejidad técnica puede existir, pero no debe invadir la lectura normal.
- Las capacidades incompletas deben mostrarse como preparación/paquete/pendiente, no como producto final.
- Las acciones comunes deben pasar por componentes transversales.

## Señal detectada por smoke

El smoke T58C confirmó tests verdes y apertura de app, pero expuso advertencias CSS. Se agregaron aliases en `tokens.css` y un guardarraíl fuente para preservar esos aliases mientras se completa el rediseño visual.

## Consecuencia para próximas tandas

T59A debe enfocarse en componentes GUI transversales. T59B debe limpiar Documento. T60 debe refactorizar coordinadores solo después de fijar la UX objetivo.
