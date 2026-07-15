# T121 — Planificación de Vista Voces UX/UI final

La planificación detallada de la Vista Voces queda en:

```text
docs/productizacion/voces_ux_final/
```

## Resumen

Esta planificación define la reconstrucción de la vista Voces como módulo moderno, limpio y sobrio para administrar voces y muestras. El foco de producto es la **Voz IA avanzada**; la palabra `Coqui` no debe aparecer en la UX/UI. Piper queda como **Voz local simple** con opciones mínimas. Mock queda como **Modo de prueba**.

## Tandas

1. T121-V01 — Deshuesamiento de la vista Voces actual.
2. T121-V02 — Modelo de dominio para voces, muestras y tonos teatrales.
3. T121-V03 — Almacenamiento seguro y descarga de muestras.
4. T121-V04 — Wizard de registro de voz avanzada.
4B. T121-V04B — Persistencia real de muestras por tono.
4C. T121-V04C — Cableado visual mínimo del wizard por tono.
5. T121-V05 — Prueba generada con frase editable.
6. T121-V06 — Piper modo mínimo.
7. T121-V07 — Fallback de tonos faltantes en Documento.
8. T121-V08 — Rediseño visual final de VoiceLibraryWorkspaceView.
9. T121-V09 — Componentes GUI y CSS para Voces.
10. T121-V10 — Tests, documentación y smoke visual de Voces.


## Ajuste agregado — Frase guía por tono

Cada tono del catálogo teatral extendido debe tener una frase guía por defecto para grabación. Al grabar, la aplicación muestra esa frase, el usuario la lee, y luego puede cancelar, detener o guardar la muestra. Esto aplica a Neutral y a todos los tonos del catálogo.
