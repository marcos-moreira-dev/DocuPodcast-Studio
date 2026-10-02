package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

/** One frame in the lightweight video export plan. */
public record SimpleVideoFrame(
        String id,
        String segmentId,
        String title,
        String narrationPreview,
        String imageAssetId,
        String imageRelativePath,
        String audioRelativePath,
        double audioDurationSeconds,
        double silenceAfterSeconds,
        boolean imageAssigned,
        boolean audioReady,
        boolean silentVisual,
        List<CharacterLabel> characterLabels,
        List<VisualPart> visualParts,
        NarratedFrameBinding visualBinding
) {
    public SimpleVideoFrame {
        id = token(id, "id");
        segmentId = token(segmentId, "segmentId");
        title = title == null || title.isBlank() ? segmentId : title.strip();
        narrationPreview = narrationPreview == null ? "" : narrationPreview.strip();
        imageAssetId = imageAssetId == null ? "" : imageAssetId.strip();
        imageRelativePath = optionalPortablePath(imageRelativePath);
        audioRelativePath = optionalPortablePath(audioRelativePath);
        audioDurationSeconds = Math.max(0.0, audioDurationSeconds);
        silenceAfterSeconds = Math.max(0.0, silenceAfterSeconds);
        characterLabels = characterLabels == null ? List.of() : List.copyOf(characterLabels);
        if (!imageRelativePath.isBlank()) {
            imageAssigned = true;
        }
        if (!audioRelativePath.isBlank()) {
            audioReady = true;
        }
        if (silentVisual) {
            audioReady = false;
            audioDurationSeconds = 0.0;
        }
        visualParts = normalizeVisualParts(visualParts, imageAssetId, imageRelativePath,
                audioDurationSeconds + silenceAfterSeconds);
        if (!visualParts.isEmpty()) {
            imageAssigned = true;
            if (imageRelativePath.isBlank()) {
                imageRelativePath = visualParts.getFirst().imageRelativePath();
            }
        }
        if (visualBinding != null) {
            if (!id.equals(visualBinding.frameId())) {
                throw new IllegalArgumentException("visualBinding.frameId must match frame id");
            }
            if (!segmentId.equals(visualBinding.segmentId())) {
                throw new IllegalArgumentException("visualBinding.segmentId must match segment id");
            }
        }
    }

    public SimpleVideoFrame(
            String id, String segmentId, String title, String narrationPreview,
            String imageAssetId, String imageRelativePath, String audioRelativePath,
            double audioDurationSeconds, double silenceAfterSeconds,
            boolean imageAssigned, boolean audioReady, boolean silentVisual,
            List<CharacterLabel> characterLabels, List<VisualPart> visualParts) {
        this(id, segmentId, title, narrationPreview, imageAssetId, imageRelativePath,
                audioRelativePath, audioDurationSeconds, silenceAfterSeconds,
                imageAssigned, audioReady, silentVisual, characterLabels, visualParts, null);
    }

    public SimpleVideoFrame(
            String id,
            String segmentId,
            String title,
            String narrationPreview,
            String imageAssetId,
            String imageRelativePath,
            String audioRelativePath,
            double audioDurationSeconds,
            double silenceAfterSeconds,
            boolean imageAssigned,
            boolean audioReady
    ) {
        this(id, segmentId, title, narrationPreview, imageAssetId, imageRelativePath, audioRelativePath,
                audioDurationSeconds, silenceAfterSeconds, imageAssigned, audioReady, false, List.of(), List.of(), null);
    }

    public SimpleVideoFrame(
            String id,
            String segmentId,
            String title,
            String narrationPreview,
            String imageAssetId,
            String imageRelativePath,
            String audioRelativePath,
            double audioDurationSeconds,
            double silenceAfterSeconds,
            boolean imageAssigned,
            boolean audioReady,
            boolean silentVisual
    ) {
        this(id, segmentId, title, narrationPreview, imageAssetId, imageRelativePath, audioRelativePath,
                audioDurationSeconds, silenceAfterSeconds, imageAssigned, audioReady, silentVisual, List.of(), List.of(), null);
    }

    public SimpleVideoFrame(
            String id,
            String segmentId,
            String title,
            String narrationPreview,
            String imageAssetId,
            String imageRelativePath,
            String audioRelativePath,
            double audioDurationSeconds,
            double silenceAfterSeconds,
            boolean imageAssigned,
            boolean audioReady,
            boolean silentVisual,
            List<CharacterLabel> characterLabels
    ) {
        this(id, segmentId, title, narrationPreview, imageAssetId, imageRelativePath, audioRelativePath,
                audioDurationSeconds, silenceAfterSeconds, imageAssigned, audioReady, silentVisual, characterLabels, List.of(), null);
    }

    public record CharacterLabel(String characterName, double x, double y) {
    }

    public enum VisualPartKind {
        STILL_IMAGE,
        VIDEO_CLIP
    }

    public record VisualPart(
            VisualPartKind kind,
            String imageAssetId,
            String imageRelativePath,
            double durationSeconds,
            double sourceStartSeconds,
            String label
    ) {
        public VisualPart {
            kind = Objects.requireNonNullElse(kind, VisualPartKind.STILL_IMAGE);
            imageAssetId = imageAssetId == null ? "" : imageAssetId.strip();
            imageRelativePath = optionalPortablePath(imageRelativePath);
            durationSeconds = Math.max(0.0, durationSeconds);
            sourceStartSeconds = Math.max(0.0, sourceStartSeconds);
            label = label == null ? "" : label.strip();
        }

        public VisualPart(String imageAssetId, String imageRelativePath, double durationSeconds, String label) {
            this(VisualPartKind.STILL_IMAGE, imageAssetId, imageRelativePath, durationSeconds, 0.0, label);
        }

        public static VisualPart videoClip(String assetId,
                                           String relativePath,
                                           double durationSeconds,
                                           double sourceStartSeconds,
                                           String label) {
            return new VisualPart(VisualPartKind.VIDEO_CLIP, assetId, relativePath,
                    durationSeconds, sourceStartSeconds, label);
        }

        public boolean videoClip() {
            return kind == VisualPartKind.VIDEO_CLIP;
        }
    }

    public double frameDurationSeconds() {
        return audioDurationSeconds + silenceAfterSeconds;
    }

    public boolean audioRequired() {
        return !silentVisual;
    }

    public boolean missingRequiredAudio() {
        return audioRequired() && !audioReady;
    }

    public String frameModeLabel() {
        if (silentVisual) {
            return "visual-silencioso";
        }
        return audioReady ? "imagen-audio" : "requiere-audio";
    }

    public String imageLabel() {
        return imageAssigned ? imageRelativePath : "sin-imagen";
    }

    public String audioLabel() {
        if (silentVisual) {
            return "silencio";
        }
        return audioReady ? audioRelativePath : "sin-audio";
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String optionalPortablePath(String value) {
        String normalized = Objects.toString(value, "").replace('\\', '/').strip();
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("../")
                || normalized.contains("/../") || normalized.startsWith("./") || normalized.contains("://")
                || normalized.toLowerCase(java.util.Locale.ROOT).startsWith("file:")) {
            throw new IllegalArgumentException("Path must be project-relative: " + value);
        }
        return normalized;
    }

    private static List<VisualPart> normalizeVisualParts(List<VisualPart> parts,
                                                         String imageAssetId,
                                                         String imageRelativePath,
                                                         double totalDurationSeconds) {
        ArrayList<VisualPart> normalized = new ArrayList<>();
        if (parts != null) {
            for (VisualPart part : parts) {
                if (part != null && !part.imageRelativePath().isBlank() && part.durationSeconds() > 0.0) {
                    normalized.add(part);
                }
            }
        }
        if (normalized.isEmpty() && imageRelativePath != null && !imageRelativePath.isBlank()
                && totalDurationSeconds > 0.0) {
            normalized.add(new VisualPart(imageAssetId, imageRelativePath, totalDurationSeconds, "principal"));
        }
        return List.copyOf(normalized);
    }
}
