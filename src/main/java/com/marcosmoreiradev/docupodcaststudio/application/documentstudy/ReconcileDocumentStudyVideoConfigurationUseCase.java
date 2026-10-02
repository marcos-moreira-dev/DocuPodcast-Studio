package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Consolidates inference-era PDF content keys into stable semantic content IDs.
 * Legacy IDs are accepted only as read aliases and are removed when the
 * configuration is materialized again.
 */
public final class ReconcileDocumentStudyVideoConfigurationUseCase {

    public DocumentStudyVideoConfiguration execute(
            DocumentContentProjection projection,
            DocumentStudyVideoConfiguration configuration) {
        Objects.requireNonNull(projection, "projection");
        DocumentStudyVideoConfiguration safe = configuration == null
                ? DocumentStudyVideoConfiguration.empty() : configuration;

        ArrayList<DocumentVideoSlideConfiguration> slides =
                new ArrayList<>(safe.contentSlides());
        LinkedHashSet<String> disabled = new LinkedHashSet<>(safe.disabledBlockIds());
        for (DocumentContentItem content : projection.items()) {
            reconcileSlide(content, slides);
            reconcileDisabledId(content, slides, disabled);
        }
        return new DocumentStudyVideoConfiguration(
                safe.videoTitle(), safe.defaultTableDurationSeconds(),
                safe.paragraphVisuals(), safe.tableSlides(), safe.musicTracks(),
                List.copyOf(disabled), safe.closingSlides(), List.copyOf(slides),
                safe.secondarySlideInclusionMode(), safe.aiIllustrationsEnabled(), safe.aiIllustrationAppearance());
    }

    public static Optional<DocumentVideoSlideConfiguration> configuredSlide(
            DocumentContentItem content,
            DocumentStudyVideoConfiguration configuration) {
        if (content == null || configuration == null) return Optional.empty();
        Optional<DocumentVideoSlideConfiguration> canonical =
                configuration.content(content.contentId());
        if (canonical.isPresent()) return canonical;
        for (String legacyId : content.legacyContentIds()) {
            Optional<DocumentVideoSlideConfiguration> legacy =
                    configuration.content(legacyId);
            if (legacy.isPresent()) return legacy;
        }
        return Optional.empty();
    }

    public static boolean blockEnabled(
            DocumentContentItem content,
            DocumentStudyVideoConfiguration configuration) {
        if (content == null || configuration == null) return true;
        Optional<DocumentVideoSlideConfiguration> slide =
                configuredSlide(content, configuration);
        if (slide.isPresent()) return slide.orElseThrow().enabled();
        if (!configuration.blockEnabled(content.contentId())) return false;
        return content.legacyContentIds().stream()
                .allMatch(configuration::blockEnabled);
    }

    private static void reconcileSlide(
            DocumentContentItem content,
            ArrayList<DocumentVideoSlideConfiguration> slides) {
        DocumentVideoSlideConfiguration canonical = slides.stream()
                .filter(slide -> slide.contentId().equals(content.contentId()))
                .findFirst().orElse(null);
        DocumentVideoSlideConfiguration legacy = canonical == null
                ? slides.stream()
                .filter(slide -> content.legacyContentIds()
                        .contains(slide.contentId()))
                .findFirst().orElse(null)
                : null;
        Set<String> aliases = Set.copyOf(content.legacyContentIds());
        slides.removeIf(slide -> aliases.contains(slide.contentId()));
        if (canonical == null && legacy != null) {
            slides.add(rekey(content, legacy));
        }
    }

    private static void reconcileDisabledId(
            DocumentContentItem content,
            ArrayList<DocumentVideoSlideConfiguration> slides,
            LinkedHashSet<String> disabled) {
        boolean disabledByLegacy = content.legacyContentIds().stream()
                .anyMatch(disabled::contains);
        disabled.removeAll(content.legacyContentIds());
        if (disabledByLegacy) {
            disabled.add(content.contentId());
            for (int index = 0; index < slides.size(); index++) {
                DocumentVideoSlideConfiguration slide = slides.get(index);
                if (slide.contentId().equals(content.contentId())
                        && slide.enabled()) {
                    slides.set(index, slide.withEnabled(false));
                }
            }
        }
    }

    private static DocumentVideoSlideConfiguration rekey(
            DocumentContentItem content,
            DocumentVideoSlideConfiguration legacy) {
        return new DocumentVideoSlideConfiguration(
                content.contentId(), legacy.sourceFingerprint(), legacy.visual(),
                legacy.durationSeconds(), legacy.enabled(),
                legacy.sourceVisualAssetId(), legacy.sourceVisualFingerprint());
    }
}
