# VOZ-TTS5A — Documento con voces/tonos reales

Documento ya no muestra el catálogo global de tonos: filtra voces avanzadas por muestra Neutral y tonos por `registeredTones()` de la voz seleccionada. Documento técnico: `docs/productizacion/VOZ_TTS5A_DOCUMENTO_VOCES_TONOS_REALES.md`.

# Nota vigente — VOZ-UX4R-DOC1

La documentación de Vista Voces queda alineada en `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md`: microaplicación administrativa sobria con módulos Inicio, Configurar motor y Gestionar voces; muchas emociones; Neutral obligatoria; selector CPU/GPU real para todos los motores; y Documento filtrando voces/emociones registradas.

# 00 — Léeme primero

Esta carpeta es el traspaso largo del proyecto **DocuPodcast Studio**. La intención es que el repositorio no dependa de una ventana de chat larga que eventualmente se va a volver inmanejable. Todo lo conversado queda condensado aquí: visión de producto, decisiones técnicas, referencias a proyectos existentes, arquitectura esperada, riesgos, guardarraíles, workspaces, persistencia, audio, storyboard, Word/DOCX, Markdown, exportaciones y plan de implementación.

DocuPodcast Studio no debe entenderse como un “mini Word”. La formulación correcta es:

> Aplicación JavaFX autocontenida para convertir documentos Word y guiones en podcasts locales y storyboards vivos, usando guion narrable, voces, personajes, estilos, imágenes asociadas, generación de audio por segmentos y reproducción sincronizada.

La cadena principal del producto es:

```text
Word/DOCX → Documento importado → Guion narrable → Voces/estilos/storyboard → Audio por segmentos → Podcast / storyboard vivo
```

La prioridad de entrada es **Word/DOCX**, porque el usuario guarda sus notas en Word. Markdown sigue siendo importante, pero como puente humano/IA, no como fuente principal obligatoria.

El stack objetivo queda fijado así:

```text
Java: 21
Distribución: Eclipse Temurin
Build: Maven con Toolchain
UI: JavaFX
Arquitectura: domain / application / infrastructure / presentation / bootstrap
```

Referencias principales:

- **Domain Model Studio/UENS**: scaffolding principal de shell, tabs, toolbar contextual, SideDock, workspaces, tema claro, guía, Markdown, assets, exportaciones y tests.
- **Fractal Render Studio**: referencia principal de jobs largos, progreso, cola batch, cancelación, métricas y generación por lotes.

El repositorio actual es una primera tanda de onboarding: no intenta implementar todo el producto, sino dejar la base para que el siguiente agente o siguiente chat pueda avanzar sin perder contexto.
