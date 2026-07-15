# Riesgos técnicos

## Motor TTS

Riesgo: empaquetado, GPU, modelos pesados, dependencias nativas.

Mitigación:

- gateway;
- worker interno;
- mock audio;
- diagnóstico;
- no API visible.

## Word mal formateado

Riesgo: estilos inconsistentes.

Mitigación:

- perfil de lectura;
- fallback por tamaño/negrita/numeración;
- preview de estructura;
- corrección manual.

## Audio largo

Riesgo: generación tarda minutos, falla a mitad.

Mitigación:

- segmentos;
- jobs persistentes;
- reintentos;
- ETA;
- logs.

## Promesas emocionales

Riesgo: UI promete emoción que motor no soporta.

Mitigación:

- capability policy;
- tooltips honestos;
- fallback con advertencia.

## Storyboard se vuelve editor de video

Riesgo: scope creep.

Mitigación:

- MVP imagen por segmento;
- playback simple;
- video como futuro.

## Pérdida de contexto

Riesgo: chat se rompe.

Mitigación:

- esta carpeta;
- AI_HANDOFF;
- docs vivos;
- plan de implementación detallado.
