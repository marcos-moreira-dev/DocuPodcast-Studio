package com.marcosmoreiradev.docupodcaststudio.domain.render;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

import java.util.List;
import java.util.Objects;

/**
 * Final media decision for one document/script unit.
 *
 * <p>This is the TI1 bridge that future audio/video/playback work should consume.
 * It deliberately allows visual units without narration, so source images, tables
 * and formula blocks can be rendered silently only when the user assigns a visual.</p>
 */
public record RenderUnit(
        String id,
        String sourceNarrationUnitId,
        String segmentId,
        int index,
        ScriptTextRange scriptRange,
        DocumentTextRange documentRange,
        String title,
        String text,
        RenderUnitKind kind,
        String voiceProfileId,
        String performanceStyleId,
        String audioAssetId,
        String imageAssetId,
        double silentDurationSeconds,
        List<String> appliedLayerIds
) {
    public RenderUnit {
        id = token(id, "id");
        sourceNarrationUnitId = normalize(sourceNarrationUnitId);
        segmentId = normalize(segmentId);
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        title = normalize(title);
        text = normalize(text);
        kind = kind == null ? RenderUnitKind.OMITTED : kind;
        voiceProfileId = normalize(voiceProfileId);
        performanceStyleId = normalize(performanceStyleId);
        audioAssetId = normalize(audioAssetId);
        imageAssetId = normalize(imageAssetId);
        silentDurationSeconds = silentDurationSeconds <= 0 ? 5.0 : Math.max(1.0, Math.min(60.0, silentDurationSeconds));
        appliedLayerIds = appliedLayerIds == null ? List.of() : List.copyOf(appliedLayerIds);

        if (kind.spoken()) {
            if (segmentId.isBlank()) {
                throw new IllegalArgumentException("segmentId is required for spoken render units");
            }
            if (scriptRange == null) {
                throw new IllegalArgumentException("scriptRange is required for spoken render units");
            }
            if (text.isBlank()) {
                throw new IllegalArgumentException("text is required for spoken render units");
            }
        }
        if (kind.visual() && imageAssetId.isBlank()) {
            throw new IllegalArgumentException("imageAssetId is required for visual render units");
        }
        if (kind.silentVisual() && documentRange == null) {
            throw new IllegalArgumentException("documentRange is required for visual silent units");
        }
    }

    public static RenderUnit fromNarrationUnit(NarrationRenderUnit unit, int index, double defaultSilentDurationSeconds) {
        Objects.requireNonNull(unit, "unit");
        RenderUnitKind kind = unit.hasImage() ? RenderUnitKind.SPOKEN_WITH_VISUAL : RenderUnitKind.SPOKEN_ONLY;
        return new RenderUnit(
                unit.id(),
                unit.id(),
                unit.segmentId(),
                index,
                unit.scriptRange(),
                unit.documentRange(),
                unit.segmentId(),
                unit.text(),
                kind,
                unit.voiceProfileId(),
                unit.performanceStyleId(),
                unit.audioAssetId(),
                unit.imageAssetId(),
                defaultSilentDurationSeconds,
                unit.appliedLayerIds()
        );
    }

    public static RenderUnit visualSilent(
            String id,
            int index,
            DocumentTextRange documentRange,
            String title,
            String imageAssetId,
            double silentDurationSeconds,
            List<String> appliedLayerIds
    ) {
        return new RenderUnit(id, "", "", index, null, documentRange, title, "",
                RenderUnitKind.VISUAL_SILENT, "", "", "", imageAssetId,
                silentDurationSeconds, appliedLayerIds);
    }

    public boolean requiresAudioGeneration() {
        return kind.spoken() && audioAssetId.isBlank();
    }

    public boolean usesExternalAudio() {
        return kind.spoken() && !audioAssetId.isBlank();
    }

    public boolean renderableInVideo() {
        return kind.visual();
    }

    public boolean omittedFromVideo() {
        return !renderableInVideo();
    }

    public boolean silentVideoFrame() {
        return kind.silentVisual();
    }

    public double effectiveVisualDurationSeconds() {
        return kind.silentVisual() ? silentDurationSeconds : 0.0;
    }

    private static String token(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
