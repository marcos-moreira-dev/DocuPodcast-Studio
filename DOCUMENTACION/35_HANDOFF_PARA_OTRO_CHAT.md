# 35 — Handoff para otro chat o agente

Contexto mínimo para continuar:

DocuPodcast Studio es una app JavaFX autocontenida para convertir notas Word y guiones en podcasts locales y storyboards vivos.

Stack cerrado:

```text
Java 21
Eclipse Temurin
Maven Toolchain
JavaFX
```

Prioridad:

```text
Word/DOCX primero.
Markdown después como puente IA.
Audio por segmentos.
Proyecto portable.
```

Referencias:

```text
DMS = scaffolding UI/arquitectura.
Fractal = jobs largos/progreso.
```

Próximo paso recomendado:

```text
Tanda 2 — Proyecto .docupodcast.json mínimo + assets relativos.
```

No repetir:

- No convertirlo en mini Word.
- No usar canvas para guion.
- No prometer película.
- No mostrar emociones si motor no soporta.
- No acoplar UI a TTS.

Regla de oro:

```text
Texto ↔ voz ↔ estilo ↔ imagen ↔ audio ↔ job
```

La app debe preservar esa trazabilidad.
