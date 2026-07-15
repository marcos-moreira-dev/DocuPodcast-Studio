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
        List<VisualPart> visualParts
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
                audioDurationSeconds, silenceAfterSeconds, imageAssigned, audioReady, false, List.of(), List.of());
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
                audioDurationSeconds, silenceAfterSeconds, imageAssigned, audioReady, silentVisual, List.of(), List.of());
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
                audioDurationSeconds, silenceAfterSeconds, imageAssigned, audioReady, silentVisual, characterLabels, List.of());
    }

    public record CharacterLabel(String characterName, double x, double y) {
    }

    public record VisualPart(String imageAssetId, String imageRelativePath, double durationSeconds, String label) {
        public VisualPart {
            imageAssetId = imageAssetId == null ? "" : imageAssetId.strip();
            imageRelativePath = optionalPortablePath(imageRelativePath);
            durationSeconds = Math.max(0.0, durationSeconds);
            label = label == null ? "" : label.strip();
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
