# Estrategia — Tanda 8 Gateway TTS real

## Decisión

DocuPodcast Studio usará un gateway de audio intercambiable. La primera implementación real no es un backend ni API: es un proceso local configurado por comando.

## Beneficios

- Permite usar XTTS, Piper o un worker Python empaquetado.
- No obliga a instalar Python manualmente si luego se distribuye como `.exe`.
- Mantiene JavaFX autocontenida como experiencia visible.
- Permite seguir usando mock cuando no hay motor real.
- Reutiliza la persistencia de jobs y reanudación implementada en 7B/7C.

## Próximo riesgo técnico

El worker concreto debe producir WAVs correctos y soportar textos por segmento. La Tanda 9 debe probar reanudación con motor real y fallos reales de proceso.
