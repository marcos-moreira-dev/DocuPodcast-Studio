# Handoff largo para otro chat/agente

DocuPodcast Studio debe continuarse como app JavaFX autocontenida con Java 21 Temurin + Maven Toolchain.

## Prioridad del usuario

El usuario quiere convertir notas Word en audio de voz natural para estudiar con menor fricción. También quiere evolucionar a guiones teatrales/cine con voces, estilos y storyboard vivo.

## Qué debe hacerse primero

No comenzar por TTS real ni storyboard. Primero:

1. scaffolding estable;
2. proyecto `.docupodcast.json`;
3. importador DOCX;
4. documento importado;
5. guion narrable;
6. audio mock con progreso;
7. audio real.

## Cuidado principal

No prometer más de lo implementado. Si una feature no tiene cadena completa, debe quedar como placeholder documentado, no como botón activo.

## Referencias

- DMS: UI/arquitectura.
- Fractal: jobs/progreso.
- Proyecto IA/TTS previo: motor de voz local.

## Próxima tanda sugerida

Tanda 2: `.docupodcast.json` mínimo + assets relativos + tests.
