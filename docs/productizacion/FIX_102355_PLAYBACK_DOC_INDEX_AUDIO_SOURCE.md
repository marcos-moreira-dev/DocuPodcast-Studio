# FIX 102355 + PLAYBACK-CORE6 + DOC-INDEX-UX-HF1 + DOC-AUDIO-SOURCE-HF1

## Corrección del diagnóstico 20260607-102355

El diagnóstico fallaba por dos guardarraíles fuente:

- `IconIndexAndRibbonLabelsHf1SourceTest`: el botón transversal `RibbonButton` conservaba el ancho base de T101 pero no exponía el ancho ampliado que evita truncar `Reproducir desde aquí`.
- `ProjectIntegrityUx1SourceTest`: el test todavía buscaba la traducción de integridad dentro de `DocuPodcastShellViewModel`, aunque la responsabilidad ya se había movido al `ProjectWorkflowCoordinator`.

Se corrigió manteniendo compatibilidad con el contrato anterior del ribbon y alineando el test de integridad con la extracción de coordinador.

## PLAYBACK-CORE6

La reproducción secuencial queda protegida por la cola `PlaybackSequentialQueueDriver` como autoridad de continuidad. Mientras la cola está activa, el tick de JavaFX no decide el siguiente cue y el watchdog de cue no se arma desde `playExactCue`; el avance viene del callback real del reproductor o del watchdog interno de la cola.

Propósito operativo: evitar repeticiones y cortes al reproducir desde una oración o desde el documento mientras se encadenan chunks WAV.

## DOC-INDEX-UX-HF1

El módulo Índice refuerza el lenguaje de árbol documental: raíz, secciones y hojas. El icono sigue usando `INDEX_TREE` y el tooltip del módulo explica que sirve para saltar por el documento sin editar la fuente.

Propósito operativo: que el usuario entienda que el módulo no es un diagrama técnico sino una navegación jerárquica del documento.

## DOC-AUDIO-SOURCE-HF1

El submódulo Audio del Documento muestra una nota operativa junto al origen. Solo lista fuentes usables para Documento y aclara que Voz IA avanzada no aparece si no pasó prueba WAV.

Propósito operativo: evitar que el usuario crea que un motor configurado pero roto puede usarse en la selección actual.
