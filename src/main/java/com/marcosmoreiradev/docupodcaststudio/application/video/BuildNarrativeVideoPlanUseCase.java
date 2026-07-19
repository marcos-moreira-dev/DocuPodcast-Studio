package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeGeneratedClip;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeParagraphTake;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Builds the narrative MP4 plan from persisted paragraph takes and generated I2V clips. */
public final class BuildNarrativeVideoPlanUseCase {
    private static final double DURATION_TOLERANCE_SECONDS = 0.08;

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory) throws IOException {
        Objects.requireNonNull(project, "project");
        if (script == null || script.empty()) {
            throw new IOException("Prepara la voz en off antes de exportar el video narrativo.");
        }
        Path root = Objects.requireNonNull(projectDirectory, "projectDirectory")
                .toAbsolutePath().normalize();
        NarrativeProjectLayer narrative = project.narrative();
        ProjectAssetCatalog assets = project.assets();
        Map<String, AudioSegmentSnapshot> audio = completedAudioBySegment(jobs, root);
        LinkedHashMap<String, List<NarrationSegment>> segmentsByBlock = segmentsByBlock(script);
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        Set<String> processedBlocks = new LinkedHashSet<>();
        int frameIndex = 1;

        for (NarrationSegment scriptSegment : script.segments()) {
            if (secondaryReadUnit(scriptSegment)) {
                continue;
            }
            for (String blockId : scriptSegment.sourceBlockIds()) {
                if (!processedBlocks.add(blockId)) {
                    continue;
                }
                NarrativeParagraphTake take = narrative.takeOrDefault(blockId);
                if (!take.enabled()) {
                    continue;
                }
                validateTake(take, blockId);
                List<ClipSource> clipSources = resolveClipSources(take, assets, root);
                ClipCursor cursor = new ClipCursor(clipSources);
                List<NarrationSegment> blockSegments = segmentsByBlock.getOrDefault(blockId, List.of());
                if (blockSegments.isEmpty()) {
                    throw new IOException("El parrafo " + blockId
                            + " esta habilitado, pero no tiene una unidad de narracion preparada.");
                }
                double requiredDuration = requiredAudioDuration(blockSegments, audio, root);
                if (take.generatedDurationSeconds() + DURATION_TOLERANCE_SECONDS < requiredDuration) {
                    throw new IOException("La toma " + blockId + " solo tiene "
                            + format(take.generatedDurationSeconds()) + " s de video para "
                            + format(requiredDuration) + " s de voz. Regenera sus clips.");
                }
                for (NarrationSegment segment : blockSegments) {
                    List<AudioSegmentSnapshot> units = audioForSegment(audio, segment.id());
                    if (units.isEmpty()) {
                        throw new IOException("Falta el audio de la toma " + blockId
                                + " (" + segment.id() + ").");
                    }
                    for (AudioSegmentSnapshot unit : units) {
                        List<SimpleVideoFrame.VisualPart> visualParts =
                                cursor.consume(unit.durationSeconds(), blockId);
                        String keyframePath = assetPath(assets, take.keyframeAssetId(), root,
                                "imagen clave", blockId);
                        frames.add(new SimpleVideoFrame(
                                "NARRATIVE-" + String.format(java.util.Locale.ROOT, "%04d", frameIndex++),
                                unit.segmentId(),
                                segment.title().isBlank() ? blockId : segment.title(),
                                segment.narrationText(),
                                take.keyframeAssetId(),
                                keyframePath,
                                unit.audioRelativePath(),
                                unit.durationSeconds(),
                                0.0,
                                true,
                                true,
                                false,
                                List.of(),
                                visualParts));
                    }
                }
            }
        }
        if (frames.isEmpty()) {
            throw new IOException("No hay parrafos habilitados y listos para exportar el video narrativo.");
        }
        return new SimpleVideoPlan("Video narrativo - " + script.title(), frames, 0.0, Instant.now());
    }

    private static void validateTake(NarrativeParagraphTake take, String blockId) throws IOException {
        if (!take.keyframeReady()) {
            throw new IOException("El parrafo " + blockId + " no tiene imagen clave.");
        }
        if (take.stale()) {
            throw new IOException("La toma " + blockId
                    + " esta desactualizada. Regenera su imagen clave y sus clips.");
        }
        if (!take.clipsReady()) {
            throw new IOException("El parrafo " + blockId + " no tiene clips de video vigentes.");
        }
    }

    private static List<ClipSource> resolveClipSources(NarrativeParagraphTake take,
                                                       ProjectAssetCatalog assets,
                                                       Path root) throws IOException {
        ArrayList<ClipSource> result = new ArrayList<>();
        for (NarrativeGeneratedClip clip : take.clips()) {
            String path = assetPath(assets, clip.assetId(), root, "clip narrativo", take.blockId());
            result.add(new ClipSource(clip, path));
        }
        return List.copyOf(result);
    }

    private static String assetPath(ProjectAssetCatalog assets,
                                    String assetId,
                                    Path root,
                                    String label,
                                    String blockId) throws IOException {
        ProjectAssetReference asset = assets.byId(assetId)
                .orElseThrow(() -> new IOException("No se encontro el asset de " + label
                        + " para " + blockId + ": " + assetId));
        Path file = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new IOException("El archivo de " + label + " para " + blockId
                    + " no existe dentro del proyecto: " + asset.relativePath());
        }
        return asset.relativePath();
    }

    private static LinkedHashMap<String, List<NarrationSegment>> segmentsByBlock(
            NarrationScriptDocument script) {
        LinkedHashMap<String, ArrayList<NarrationSegment>> mutable = new LinkedHashMap<>();
        for (NarrationSegment segment : script.segments()) {
            if (secondaryReadUnit(segment)) {
                continue;
            }
            for (String blockId : segment.sourceBlockIds()) {
                mutable.computeIfAbsent(blockId, ignored -> new ArrayList<>()).add(segment);
            }
        }
        LinkedHashMap<String, List<NarrationSegment>> result = new LinkedHashMap<>();
        mutable.forEach((blockId, segments) -> result.put(blockId, List.copyOf(segments)));
        return result;
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioBySegment(
            List<AudioJobSnapshot> jobs,
            Path root) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (jobs == null) {
            return result;
        }
        jobs.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AudioJobSnapshot::updatedAt))
                .forEach(job -> job.segments().stream()
                        .filter(segment -> usableAudio(root, segment))
                        .forEach(segment -> result.put(segment.segmentId(), segment)));
        return result;
    }

    private static boolean usableAudio(Path root, AudioSegmentSnapshot segment) {
        if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank()
                || segment.durationSeconds() <= 0.0) {
            return false;
        }
        Path file = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        return file.startsWith(root) && Files.isRegularFile(file);
    }

    private static List<AudioSegmentSnapshot> audioForSegment(
            Map<String, AudioSegmentSnapshot> audio,
            String segmentId) {
        AudioSegmentSnapshot exact = audio.get(segmentId);
        if (exact != null) {
            return List.of(exact);
        }
        return audio.values().stream()
                .filter(clip -> clip.segmentId().startsWith(segmentId + "-"))
                .sorted(Comparator.comparing(AudioSegmentSnapshot::segmentId))
                .toList();
    }

    private static double requiredAudioDuration(List<NarrationSegment> segments,
                                                Map<String, AudioSegmentSnapshot> audio,
                                                Path root) throws IOException {
        double total = 0.0;
        for (NarrationSegment segment : segments) {
            List<AudioSegmentSnapshot> units = audioForSegment(audio, segment.id());
            if (units.isEmpty()) {
                throw new IOException("Falta el audio preparado para " + segment.id() + ".");
            }
            for (AudioSegmentSnapshot unit : units) {
                if (!usableAudio(root, unit)) {
                    throw new IOException("El audio de " + unit.segmentId()
                            + " no es valido dentro del proyecto.");
                }
                total += unit.durationSeconds();
            }
        }
        return total;
    }

    private static boolean secondaryReadUnit(NarrationSegment segment) {
        return segment != null
                && Boolean.parseBoolean(segment.metadata().getOrDefault("secondaryReadUnit", "false"));
    }

    private static String format(double seconds) {
        return String.format(java.util.Locale.ROOT, "%.2f", seconds);
    }

    private record ClipSource(NarrativeGeneratedClip clip, String relativePath) {
    }

    private static final class ClipCursor {
        private final List<ClipSource> clips;
        private int index;
        private double offset;

        private ClipCursor(List<ClipSource> clips) {
            this.clips = clips;
        }

        private List<SimpleVideoFrame.VisualPart> consume(double requestedSeconds,
                                                          String blockId) throws IOException {
            ArrayList<SimpleVideoFrame.VisualPart> parts = new ArrayList<>();
            double remaining = requestedSeconds;
            while (remaining > DURATION_TOLERANCE_SECONDS) {
                if (index >= clips.size()) {
                    throw new IOException("Los clips de " + blockId
                            + " no cubren toda la duracion de la voz.");
                }
                ClipSource source = clips.get(index);
                double available = Math.max(0.0, source.clip().durationSeconds() - offset);
                if (available <= DURATION_TOLERANCE_SECONDS) {
                    index++;
                    offset = 0.0;
                    continue;
                }
                double duration = Math.min(remaining, available);
                parts.add(SimpleVideoFrame.VisualPart.videoClip(
                        source.clip().assetId(),
                        source.relativePath(),
                        duration,
                        offset,
                        source.clip().id()));
                remaining -= duration;
                offset += duration;
                if (source.clip().durationSeconds() - offset <= DURATION_TOLERANCE_SECONDS) {
                    index++;
                    offset = 0.0;
                }
            }
            if (parts.isEmpty()) {
                throw new IOException("No se pudo asignar video a la voz de " + blockId + ".");
            }
            return List.copyOf(parts);
        }
    }
}
