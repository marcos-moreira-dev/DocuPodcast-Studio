# Tanda 6 — Guion narrable implementado

## Resultado estratégico

DocuPodcast Studio ya no se queda en importación Word. Ahora produce un guion narrable explícito, trazable y persistible.

## Contrato conceptual

```text
ReadableDocument
  bloques revisados y clasificados

NarrationScriptDocument
  segmentos narrables listos para audio y storyboard
```

## Regla mantenida

El guion es workspace estructurado. No se usó canvas. El canvas queda reservado para Storyboard.

## Próxima dependencia

La Tanda 7 debe consumir `NarrationSegment` como unidad de job mock:

```text
SEG-001 → audio/SEG-001.wav fake
SEG-002 → audio/SEG-002.wav fake
...
```

Esto permitirá probar progreso, ETA, cancelación y cola antes de conectar motor TTS real.
