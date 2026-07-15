# PLAYBACK-SPEED9B — hotfix de compilación

## Motivo

El diagnóstico `20260605-163524` falló en `mvn compile` por una variable local capturada desde lambda:

```text
DocuPodcastShellViewModel.java:[1185,95] local variables referenced from a lambda expression must be final or effectively final
```

La causa era que `manifest` se reasignaba y luego se capturaba dentro de una lambda que iniciaba `playbackRuntimeQueue.start(...)`.

## Corrección

Se reemplazó la lambda por una resolución explícita:

```java
Optional<PlaybackCue> selectedCue = manifest.cueForSegment(segment.get().id());
if (selectedCue.isPresent()) {
    playbackRuntimeQueue.start(manifest, selectedCue.get());
}
```

Esto conserva la lógica de `PLAYBACK-SPEED9` sin capturar una variable no efectivamente final.

## Alcance

- No cambia el comportamiento funcional de la cola runtime.
- No toca la UI de subtítulos/títulos Word.
- Solo desbloquea compilación y agrega guardarraíl fuente para evitar repetir el error.
