# Estrategia — Tanda 7C

## Por qué se agregó

La recuperación de jobs debe existir antes de conectar motores TTS reales. Los motores reales serán lentos y frágiles; por tanto, el sistema debe saber conservar segmentos ya completados y continuar solo lo faltante.

## Valor

- Reduce pérdida de trabajo.
- Hace viable procesar documentos largos.
- Prepara reintentos del motor real.
- Permite probar recuperación con WAV mock antes de costos reales.

## Capacidades futuras documentadas

Además, se documenta una línea futura importante: selección de texto con fuente de voz configurable.

```text
selección/rango de texto
  → voz IA/TTS o voz humana
  → imagen asociada opcional
  → audio grabado/importado opcional
  → reproducción o transcripción futura
```

Esta línea futura se separa de la generación TTS para evitar confundir:

- audio generado por IA;
- audio humano grabado;
- audio usado para STT/Whisper;
- audio final del podcast.
