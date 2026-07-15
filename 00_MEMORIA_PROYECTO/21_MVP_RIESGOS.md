# MVP y riesgos

## MVP funcional mínimo

El MVP debe demostrar:

```text
Word/DOCX → documento → guion → audio por segmentos → export WAV
```

## Riesgos principales

1. DOCX mal formateado.
2. Motor TTS difícil de empaquetar.
3. Tiempos largos de generación.
4. Fallos en segmentos largos.
5. Reanudación incompleta.
6. Tamaño de modelos de voz.
7. Prometer emociones que el motor no controla.
8. Sincronización texto/audio/imagen.
9. Assets perdidos por rutas absolutas.
10. UI congelada si se ejecuta trabajo pesado en JavaFX thread.

## Mitigaciones

- DOCX con diagnóstico y perfil de lectura.
- TTS detrás de gateway.
- Audio mock antes de audio real.
- Jobs persistentes.
- Assets relativos.
- Botones filtrados por capacidades reales.
- Progreso y ETA desde MVP.
