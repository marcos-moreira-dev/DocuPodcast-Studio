package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Shared data preparation and project-path checks for theatre spatial video plans. */
final class TheatreSpatialVideoPlanData {
    private TheatreSpatialVideoPlanData() {
    }

    static Map<String, List<NarrationSegment>> segmentsByBlock(NarrationScriptDocument script) {
        LinkedHashMap<String, List<NarrationSegment>> result = new LinkedHashMap<>();
        if (script == null) {
            return result;
        }
        for (NarrationSegment segment : script.segments()) {
            if (segment == null) {
                continue;
            }
            for (String blockId : segment.sourceBlockIds()) {
                if (blockId != null && !blockId.isBlank()) {
                    result.computeIfAbsent(blockId, ignored -> new ArrayList<>()).add(segment);
                }
            }
            result.putIfAbsent(segment.id(), List.of(segment));
        }
        return result;
    }

    static Optional<NarrationSegment> segmentForAudio(List<NarrationSegment> segments, String audioSegmentId) {
        if (segments == null || audioSegmentId == null || audioSegmentId.isBlank()) {
            return Optional.empty();
        }
        return segments.stream()
                .filter(segment -> audioSegmentId.equals(segment.id())
                        || audioSegmentId.startsWith(segment.id() + "-"))
                .findFirst();
    }

    static Map<String, AudioSegmentSnapshot> completedAudioBySegment(List<AudioJobSnapshot> jobs,
                                                                    Path projectDirectory) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (jobs == null || jobs.isEmpty()) {
            return result;
        }
        jobs.stream()
                .sorted(Comparator.comparing(AudioJobSnapshot::updatedAt).reversed())
                .forEach(job -> job.segments().stream()
                        .filter(segment -> usableAudio(projectDirectory, segment))
                        .forEach(segment -> result.putIfAbsent(segment.segmentId(), segment)));
        return result;
    }

    static List<AudioSegmentSnapshot> audioForSegments(Map<String, AudioSegmentSnapshot> audioBySegment,
                                                       List<NarrationSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }
        ArrayList<AudioSegmentSnapshot> result = new ArrayList<>();
        for (NarrationSegment segment : segments) {
            if (segment == null) {
                continue;
            }
            for (AudioSegmentSnapshot audio : audioForSegment(audioBySegment, segment.id())) {
                if (result.stream().noneMatch(existing -> existing.segmentId().equals(audio.segmentId()))) {
                    result.add(audio);
                }
            }
        }
        return List.copyOf(result);
    }

    static String fullInterventionText(List<NarrationSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            NarrationSegment segment = segments.get(i);
            if (segment == null || segment.narrationText().isBlank()) {
                continue;
            }
            String part = i == 0 ? segment.narrationText().strip() : stripSpeaker(segment.narrationText());
            if (part.isBlank()) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(part);
        }
        return text.toString().replaceAll("\\s+", " ").strip();
    }

    static Map<String, TheatreProjectLayer.TextActionPlacement> placementsByIntervention(
            TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.TextActionPlacement> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.TextActionPlacement placement : theatre.textActionPlacements()) {
            result.put(placement.intervencionId(), placement);
        }
        return result;
    }

    static Map<String, TheatreProjectLayer.Scene> scenesById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.Scene> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.Scene scene : theatre.scenes()) {
            result.put(scene.id(), scene);
        }
        return result;
    }

    static Map<String, String> characterNamesById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.CharacterProfile character : theatre.characters()) {
            result.put(character.id(), character.displayName());
        }
        return result;
    }

    static ProjectAssetReference requireImageAsset(ProjectAssetCatalog assets, String assetId, String label)
            throws IOException {
        if (assetId == null || assetId.isBlank()) {
            throw new IOException("Falta imagen en " + label + ".");
        }
        return optionalImageAsset(assets, assetId)
                .orElseThrow(() -> new IOException("Imagen no encontrada para " + label + ": " + assetId));
    }

    static Optional<ProjectAssetReference> optionalImageAsset(ProjectAssetCatalog assets, String assetId) {
        if (assets == null || assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        return assets.byId(assetId).filter(ProjectAssetReference::isImage);
    }

    static Path assetPath(Path projectDirectory, ProjectAssetReference asset) throws IOException {
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path path = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new IOException("No se encontro imagen para video mapa: " + asset.relativePath());
        }
        return path;
    }

    static String relativeToProject(Path projectDirectory, Path file) throws IOException {
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IOException("El frame generado quedo fuera del proyecto: " + normalized);
        }
        return root.relativize(normalized).toString().replace('\\', '/');
    }

    private static boolean usableAudio(Path projectDirectory, AudioSegmentSnapshot segment) {
        if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank()) {
            return false;
        }
        if (projectDirectory == null) {
            return true;
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path audio = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        return audio.startsWith(root) && Files.isRegularFile(audio);
    }

    private static List<AudioSegmentSnapshot> audioForSegment(
            Map<String, AudioSegmentSnapshot> audioBySegment,
            String segmentId) {
        if (audioBySegment == null || audioBySegment.isEmpty() || segmentId == null || segmentId.isBlank()) {
            return List.of();
        }
        String unitPrefix = segmentId + "-";
        ArrayList<AudioSegmentSnapshot> units = new ArrayList<>();
        for (AudioSegmentSnapshot audio : audioBySegment.values()) {
            if (segmentId.equals(audio.segmentId())) {
                return units.isEmpty() ? List.of(audio) : units;
            }
            if (audio.segmentId().startsWith(unitPrefix)) {
                units.add(audio);
            }
        }
        return units;
    }

    private static String stripSpeaker(String text) {
        String value = text == null ? "" : text.strip();
        int colon = value.indexOf(':');
        if (colon > 0 && colon <= 42) {
            return value.substring(colon + 1).strip();
        }
        return value;
    }
}
