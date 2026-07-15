# VOICE-REFERENCIAS-WIZARD-SYNC — plan operativo de voces y muestras

Este documento registra el ajuste de alcance de la Vista Voces antes de implementar `VOICE-UX-POLISH1A`.

## Decisiones fijadas

- La Vista Voces administra voces y muestras de referencia.
- Las muestras `María · Neutral`, `María · Enojada`, etc. son referencias para Coqui/XTTS, no clips finales fijos.
- Documento asigna voces a fragmentos o permite usar audio local elegido por el usuario.
- La grabación de muestras debe implementarse con Java/API de audio, no con Python.
- Las muestras se guardan en la biblioteca de voces de la app/runtime/repositorio local, no dentro de cada proyecto.
- Los ComboBox de tono/emoción muestran solo nombres simples.
- Documento muestra solo tonos realmente registrados para la voz seleccionada.
- No se agregan tarjetas dashboard sin función operativa.

## Nuevas tandas registradas

1. `VOICE-UX-POLISH1A` — limpieza visual/operativa de Vista Voces.
2. `VOICE-REGISTRATION-WIZARD1` — subvista para crear voz y grabar muestras por emoción.
3. `VOICE-LIBRARY-SYNC1` — sincronización transversal para que Documento vea voces/tonos nuevos.
4. `DOCUMENT-SIDEBAR-VOICE-UX1` — pulido final del sidebar de Documento ya conectado al flujo real.

El detalle completo queda en `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/07_VOCES_VISTA_Y_WIZARD_REFERENCIAS.md`.
