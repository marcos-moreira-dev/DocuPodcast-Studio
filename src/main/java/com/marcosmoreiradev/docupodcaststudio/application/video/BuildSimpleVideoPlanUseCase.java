package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Builds a simple video plan from render units or, for legacy projects, narration segments. */
public final class BuildSimpleVideoPlanUseCase {
    public static final double DEFAULT_SILENCE_AFTER_FRAME_SECONDS = 1.0;

    public SimpleVideoPlan build(
            NarrationScriptDocument script,
            StoryboardDocument storyboard,
            ProjectAssetCatalog assets,
            List<AudioJobSnapshot> jobs
    ) {
        return build(script, storyboard, assets, jobs, DEFAULT_SILENCE_AFTER_FRAME_SECONDS);
    }

    public SimpleVideoPlan build(
            NarrationScriptDocument script,
            StoryboardDocument storyboard,
            ProjectAssetCatalog assets,
            List<AudioJobSnapshot> jobs,
            double silenceAfterFrameSeconds
    ) {
        if (script == null || script.empty()) {
            throw new IllegalArgumentException("Se requiere una lectura preparada para exportar video simple.");
        }
        ProjectAssetCatalog assetCatalog = assets == null ? ProjectAssetCatalog.empty() : assets;
        Map<String, StoryboardBinding> bindings = bindingsBySegment(storyboard);
        Map<String, AudioSegmentSnapshot> audio = completedAudioBySegment(jobs);
        java.util.ArrayList<SimpleVideoFrame> frames = new java.util.ArrayList<>();
        int index = 1;
        for (NarrationSegment segment : script.segments()) {
            StoryboardBinding binding = bindings.get(segment.id());
            AudioSegmentSnapshot clip = audio.get(segment.id());
            String imageAssetId = binding == null ? "" : binding.imageAssetId();
            String imageRelativePath = relativePath(assetCatalog, imageAssetId);
            String frameId = "FRAME-" + String.format(java.util.Locale.ROOT, "%03d", index++);
            frames.add(new SimpleVideoFrame(
                    frameId,
                    segment.id(),
                    segment.title().isBlank() ? segment.id() : segment.title(),
                    segment.narrationText(),
                    imageAssetId,
                    imageRelativePath,
                    clip == null ? "" : clip.audioRelativePath(),
                    clip == null ? 0.0 : clip.durationSeconds(),
                    silenceAfterFrameSeconds,
                    binding != null,
                    clip != null && clip.completed()
            ));
        }
        return new SimpleVideoPlan("Video simple — " + script.title(), frames, silenceAfterFrameSeconds, Instant.now());
    }

    public SimpleVideoPlan build(
            RenderUnitPlan renderUnitPlan,
            ProjectAssetCatalog assets,
            List<AudioJobSnapshot> jobs
    ) {
        double defaultSilence = renderUnitPlan == null
                ? 5.0
                : renderUnitPlan.defaultSilentVisualDurationSeconds();
        return build(renderUnitPlan, assets, jobs, defaultSilence);
    }

    public SimpleVideoPlan build(
            RenderUnitPlan renderUnitPlan,
            ProjectAssetCatalog assets,
            List<AudioJobSnapshot> jobs,
            double fallbackSilentVisualSeconds
    ) {
        if (renderUnitPlan == null || renderUnitPlan.videoUnits().isEmpty()) {
            throw new IllegalArgumentException("Se requieren unidades visuales asignadas para exportar storyboard/video.");
        }
        ProjectAssetCatalog assetCatalog = assets == null ? ProjectAssetCatalog.empty() : assets;
        Map<String, AudioSegmentSnapshot> audio = completedAudioBySegmentOrUnit(jobs);
        java.util.ArrayList<SimpleVideoFrame> frames = new java.util.ArrayList<>();
        int index = 1;
        for (RenderUnit unit : renderUnitPlan.videoUnits()) {
            String frameId = "FRAME-" + String.format(java.util.Locale.ROOT, "%03d", index++);
            String imageRelativePath = relativePath(assetCatalog, unit.imageAssetId());
            String audioRelativePath = "";
            double audioDuration = 0.0;
            boolean audioReady = false;
            boolean silentVisual = unit.kind() == RenderUnitKind.VISUAL_SILENT;
            double silence = silentVisual
                    ? unit.silentDurationSeconds()
                    : Math.max(0.0, fallbackSilentVisualSeconds);
            if (!silentVisual) {
                if (!unit.audioAssetId().isBlank()) {
                    audioRelativePath = relativePath(assetCatalog, unit.audioAssetId());
                    audioReady = !audioRelativePath.isBlank();
                } else {
                    AudioSegmentSnapshot clip = audio.get(unit.id());
                    if (clip == null && !unit.segmentId().isBlank()) {
                        clip = audio.get(unit.segmentId());
                    }
                    if (clip != null && clip.completed()) {
                        audioRelativePath = clip.audioRelativePath();
                        audioDuration = clip.durationSeconds();
                        audioReady = !audioRelativePath.isBlank();
                    }
                }
            }
            frames.add(new SimpleVideoFrame(
                    frameId,
                    unit.id(),
                    unit.title().isBlank() ? unit.id() : unit.title(),
                    unit.text(),
                    unit.imageAssetId(),
                    imageRelativePath,
                    audioRelativePath,
                    audioDuration,
                    silence,
                    true,
                    audioReady,
                    silentVisual,
                    List.of()
            ));
        }
        return new SimpleVideoPlan("Storyboard/video — RenderUnitPlan", frames,
                renderUnitPlan.defaultSilentVisualDurationSeconds(), Instant.now());
    }

    private static String relativePath(ProjectAssetCatalog assets, String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return "";
        }
        return assets.byId(assetId).map(ProjectAssetReference::relativePath).orElse("");
    }

    private static Map<String, StoryboardBinding> bindingsBySegment(StoryboardDocument storyboard) {
        LinkedHashMap<String, StoryboardBinding> result = new LinkedHashMap<>();
        if (storyboard == null) {
            return result;
        }
        for (StoryboardBinding binding : storyboard.bindings()) {
            result.put(binding.segmentId(), binding);
        }
        return result;
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioBySegment(List<AudioJobSnapshot> jobs) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (jobs == null || jobs.isEmpty()) {
            return result;
        }
        Optional<AudioJobSnapshot> latest = jobs.stream()
                .max(Comparator.comparing(AudioJobSnapshot::updatedAt));
        latest.ifPresent(job -> {
            for (AudioSegmentSnapshot segment : job.segments()) {
                if (segment.completed() && !segment.audioRelativePath().isBlank()) {
                    result.put(segment.segmentId(), segment);
                }
            }
        });
        return result;
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioBySegmentOrUnit(List<AudioJobSnapshot> jobs) {
        return completedAudioBySegment(jobs);
    }

    public static List<SimpleVideoFrame.CharacterLabel> buildCharacterLabels(
            List<TheatreProjectLayer.TextActionPlacement> placements,
            String sceneId
    ) {
        List<SimpleVideoFrame.CharacterLabel> labels = new ArrayList<>();
        if (placements == null || sceneId == null) {
            return labels;
        }
        Map<String, String> aggregated = new HashMap<>();
        for (TheatreProjectLayer.TextActionPlacement p : placements) {
            if (sceneId.equals(p.sceneId()) && p.characterLocations() != null) {
                for (var entry : p.characterLocations().entrySet()) {
                    aggregated.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        Map<String, List<String>> byLocation = new HashMap<>();
        for (var entry : aggregated.entrySet()) {
            String loc = entry.getValue();
            byLocation.computeIfAbsent(loc, k -> new ArrayList<>()).add(entry.getKey());
        }
        for (var entry : byLocation.entrySet()) {
            String loc = entry.getKey();
            double[] xy = resolveStagePoint(loc);
            int stack = 0;
            for (String character : entry.getValue()) {
                String label = character.replace("CHR-", "").replace("-", " ");
                double labelY = xy[1] + 18 + (stack * 20.0);
                double labelX = xy[0] + (stack % 2 == 0 ? 0 : 6);
                labels.add(new SimpleVideoFrame.CharacterLabel(label, labelX, labelY));
                stack++;
            }
        }
        return labels;
    }

    private static double[] resolveStagePoint(String location) {
        if (location == null || location.isBlank()) {
            return new double[]{380, 238};
        }
        String loc = location.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (loc) {
            case "fondo izquierda" -> new double[]{95, 60};
            case "fondo centro" -> new double[]{380, 50};
            case "fondo derecha" -> new double[]{665, 60};
            case "centro izquierda" -> new double[]{95, 170};
            case "centro" -> new double[]{380, 150};
            case "centro derecha" -> new double[]{665, 170};
            case "frente izquierda" -> new double[]{95, 340};
            case "frente centro" -> new double[]{380, 340};
            case "frente derecha" -> new double[]{665, 340};
            case "hacia el publico" -> new double[]{380, 400};
            case "off" -> new double[]{380, 238};
            case "hangar centro" -> new double[]{380, 280};
            case "hangar derecha" -> new double[]{600, 300};
            case "cabina izquierda" -> new double[]{150, 130};
            case "cabina derecha" -> new double[]{550, 150};
            case "campo izquierda" -> new double[]{80, 350};
            case "campo derecha" -> new double[]{650, 340};
            default -> new double[]{380, 238};
        };
    }
}
