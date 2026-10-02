package com.marcosmoreiradev.docupodcaststudio.domain.study;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Persistent documentary slideshow configuration derived from source DOCX blocks. */
public record DocumentStudyVideoConfiguration(
        String videoTitle,
        double defaultTableDurationSeconds,
        List<DocumentParagraphVisualAssignment> paragraphVisuals,
        List<DocumentTableSlideConfiguration> tableSlides,
        List<DocumentStudyMusicTrack> musicTracks,
        List<String> disabledBlockIds,
        List<DocumentStudyClosingSlide> closingSlides,
        List<DocumentVideoSlideConfiguration> contentSlides,
        SecondarySlideInclusionMode secondarySlideInclusionMode,
        boolean aiIllustrationsEnabled,
        DocumentAiIllustrationAppearance aiIllustrationAppearance
) {
    public DocumentStudyVideoConfiguration(String videoTitle, double defaultTableDurationSeconds,
            List<DocumentParagraphVisualAssignment> paragraphVisuals, List<DocumentTableSlideConfiguration> tableSlides,
            List<DocumentStudyMusicTrack> musicTracks, List<String> disabledBlockIds,
            List<DocumentStudyClosingSlide> closingSlides, List<DocumentVideoSlideConfiguration> contentSlides,
            SecondarySlideInclusionMode secondarySlideInclusionMode, boolean aiIllustrationsEnabled) {
        this(videoTitle, defaultTableDurationSeconds, paragraphVisuals, tableSlides, musicTracks,
                disabledBlockIds, closingSlides, contentSlides, secondarySlideInclusionMode, aiIllustrationsEnabled,
                DocumentAiIllustrationAppearance.defaults());
    }

    public DocumentStudyVideoConfiguration withAiIllustrationAppearance(DocumentAiIllustrationAppearance appearance) {
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds, paragraphVisuals,
                tableSlides, musicTracks, disabledBlockIds, closingSlides, contentSlides,
                secondarySlideInclusionMode, aiIllustrationsEnabled, appearance);
    }

    public DocumentStudyVideoConfiguration(String videoTitle, double defaultTableDurationSeconds,
            List<DocumentParagraphVisualAssignment> paragraphVisuals, List<DocumentTableSlideConfiguration> tableSlides,
            List<DocumentStudyMusicTrack> musicTracks, List<String> disabledBlockIds,
            List<DocumentStudyClosingSlide> closingSlides, List<DocumentVideoSlideConfiguration> contentSlides,
            SecondarySlideInclusionMode secondarySlideInclusionMode) {
        this(videoTitle, defaultTableDurationSeconds, paragraphVisuals, tableSlides, musicTracks,
                disabledBlockIds, closingSlides, contentSlides, secondarySlideInclusionMode, false);
    }

    public DocumentStudyVideoConfiguration withAiIllustrationsEnabled(boolean enabled) {
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds, paragraphVisuals,
                tableSlides, musicTracks, disabledBlockIds, closingSlides, contentSlides, secondarySlideInclusionMode, enabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration {
        aiIllustrationAppearance = Objects.requireNonNullElse(aiIllustrationAppearance, DocumentAiIllustrationAppearance.defaults());
        videoTitle = videoTitle == null ? "" : videoTitle.strip();
        defaultTableDurationSeconds = DocumentTableSlideConfiguration.clamp(defaultTableDurationSeconds);
        paragraphVisuals = paragraphVisuals == null ? List.of() : paragraphVisuals.stream()
                .filter(Objects::nonNull).toList();
        tableSlides = tableSlides == null ? List.of() : tableSlides.stream()
                .filter(Objects::nonNull).toList();
        musicTracks = musicTracks == null ? List.of() : musicTracks.stream()
                .filter(Objects::nonNull).toList();
        disabledBlockIds = disabledBlockIds == null ? List.of() : disabledBlockIds.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        closingSlides = closingSlides == null ? List.of() : closingSlides.stream()
                .filter(Objects::nonNull).toList();
        contentSlides = contentSlides == null ? List.of() : contentSlides.stream()
                .filter(Objects::nonNull).toList();
        secondarySlideInclusionMode = Objects.requireNonNullElse(
                secondarySlideInclusionMode,
                SecondarySlideInclusionMode.FOLLOW_READING_POLICY);
        if (contentSlides.isEmpty()) {
            ArrayList<DocumentVideoSlideConfiguration> migrated = new ArrayList<>();
            for (DocumentParagraphVisualAssignment visual : paragraphVisuals) {
                migrated.add(new DocumentVideoSlideConfiguration(
                        visual.blockId(), "", visual, 0.0,
                        !disabledBlockIds.contains(visual.blockId()), "", ""));
            }
            for (DocumentTableSlideConfiguration table : tableSlides) {
                boolean alreadyPresent = migrated.stream().anyMatch(
                        slide -> slide.contentId().equals(table.blockId()));
                if (!alreadyPresent) {
                    migrated.add(new DocumentVideoSlideConfiguration(
                            table.blockId(), "", DocumentParagraphVisualAssignment.empty(table.blockId()),
                            table.durationSeconds(), !disabledBlockIds.contains(table.blockId()), "", ""));
                }
            }
            contentSlides = List.copyOf(migrated);
        }
    }

    public DocumentStudyVideoConfiguration(String videoTitle,
                                           double defaultTableDurationSeconds,
                                           List<DocumentParagraphVisualAssignment> paragraphVisuals,
                                           List<DocumentTableSlideConfiguration> tableSlides,
                                           List<DocumentStudyMusicTrack> musicTracks,
                                           List<String> disabledBlockIds,
                                           List<DocumentStudyClosingSlide> closingSlides) {
        this(videoTitle, defaultTableDurationSeconds, paragraphVisuals, tableSlides,
                musicTracks, disabledBlockIds, closingSlides, List.of(),
                SecondarySlideInclusionMode.FOLLOW_READING_POLICY);
    }

    public DocumentStudyVideoConfiguration(String videoTitle,
                                           double defaultTableDurationSeconds,
                                           List<DocumentParagraphVisualAssignment> paragraphVisuals,
                                           List<DocumentTableSlideConfiguration> tableSlides,
                                           List<DocumentStudyMusicTrack> musicTracks,
                                           List<String> disabledBlockIds,
                                           List<DocumentStudyClosingSlide> closingSlides,
                                           List<DocumentVideoSlideConfiguration> contentSlides) {
        this(videoTitle, defaultTableDurationSeconds, paragraphVisuals, tableSlides,
                musicTracks, disabledBlockIds, closingSlides, contentSlides,
                SecondarySlideInclusionMode.FOLLOW_READING_POLICY);
    }

    public DocumentStudyVideoConfiguration(String videoTitle,
                                           double defaultTableDurationSeconds,
                                           List<DocumentParagraphVisualAssignment> paragraphVisuals,
                                           List<DocumentTableSlideConfiguration> tableSlides,
                                           List<DocumentStudyMusicTrack> musicTracks) {
        this(videoTitle, defaultTableDurationSeconds, paragraphVisuals, tableSlides, musicTracks,
                List.of(), List.of(), List.of(),
                SecondarySlideInclusionMode.FOLLOW_READING_POLICY);
    }

    public DocumentStudyVideoConfiguration(String videoTitle,
                                           double defaultTableDurationSeconds,
                                           List<DocumentParagraphVisualAssignment> paragraphVisuals,
                                           List<DocumentTableSlideConfiguration> tableSlides,
                                           List<DocumentStudyMusicTrack> musicTracks,
                                           List<String> disabledBlockIds) {
        this(videoTitle, defaultTableDurationSeconds, paragraphVisuals, tableSlides, musicTracks,
                disabledBlockIds, List.of(), List.of(),
                SecondarySlideInclusionMode.FOLLOW_READING_POLICY);
    }

    public static DocumentStudyVideoConfiguration empty() {
        return new DocumentStudyVideoConfiguration("", 6.0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                SecondarySlideInclusionMode.FOLLOW_READING_POLICY);
    }

    public Optional<DocumentParagraphVisualAssignment> paragraph(String blockId) {
        return paragraphVisuals.stream().filter(item -> item.blockId().equals(blockId)).findFirst();
    }

    public double tableDuration(String blockId) {
        return tableSlides.stream().filter(item -> item.blockId().equals(blockId)).findFirst()
                .map(DocumentTableSlideConfiguration::durationSeconds).orElse(defaultTableDurationSeconds);
    }

    public boolean blockEnabled(String blockId) {
        if (blockId == null) return true;
        return content(blockId).map(DocumentVideoSlideConfiguration::enabled)
                .orElse(!disabledBlockIds.contains(blockId.strip()));
    }

    public Optional<DocumentVideoSlideConfiguration> content(String contentId) {
        if (contentId == null || contentId.isBlank()) return Optional.empty();
        String normalized = contentId.strip();
        return contentSlides.stream().filter(item -> item.contentId().equals(normalized)).findFirst();
    }

    public Optional<DocumentStudyClosingSlide> closingSlide(String id) {
        return closingSlides.stream().filter(item -> item.id().equals(id)).findFirst();
    }

    public DocumentStudyVideoConfiguration withTitle(String title) {
        return new DocumentStudyVideoConfiguration(title, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, disabledBlockIds, closingSlides,
                contentSlides, secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withDefaultTableDuration(double seconds) {
        return new DocumentStudyVideoConfiguration(videoTitle, seconds, paragraphVisuals, tableSlides, musicTracks,
                disabledBlockIds, closingSlides, contentSlides, secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public double defaultSecondarySemanticDurationSeconds() {
        return defaultTableDurationSeconds;
    }

    public DocumentStudyVideoConfiguration withDefaultSecondarySemanticDuration(
            double seconds) {
        return withDefaultTableDuration(seconds);
    }

    public DocumentStudyVideoConfiguration withSecondarySlideInclusionMode(
            SecondarySlideInclusionMode mode) {
        return new DocumentStudyVideoConfiguration(videoTitle,
                defaultTableDurationSeconds, paragraphVisuals, tableSlides,
                musicTracks, disabledBlockIds, closingSlides, contentSlides,
                mode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withParagraph(DocumentParagraphVisualAssignment assignment) {
        ArrayList<DocumentParagraphVisualAssignment> updated = new ArrayList<>(paragraphVisuals);
        updated.removeIf(item -> item.blockId().equals(assignment.blockId()));
        updated.add(assignment);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                updated, tableSlides, musicTracks, disabledBlockIds, closingSlides,
                replaceContentVisual(contentSlides, assignment), secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withTable(DocumentTableSlideConfiguration table) {
        ArrayList<DocumentTableSlideConfiguration> updated = new ArrayList<>(tableSlides);
        updated.removeIf(item -> item.blockId().equals(table.blockId()));
        updated.add(table);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, updated, musicTracks, disabledBlockIds, closingSlides,
                replaceContentDuration(contentSlides, table.blockId(), table.durationSeconds()),
                secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withoutTable(String blockId) {
        ArrayList<DocumentTableSlideConfiguration> updated = new ArrayList<>(tableSlides);
        updated.removeIf(item -> item.blockId().equals(blockId));
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, updated, musicTracks, disabledBlockIds, closingSlides,
                contentSlides, secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withMusicTracks(List<DocumentStudyMusicTrack> tracks) {
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, tracks, disabledBlockIds, closingSlides,
                contentSlides, secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withBlockEnabled(String blockId, boolean enabled) {
        String normalized = blockId == null ? "" : blockId.strip();
        if (normalized.isBlank()) return this;
        ArrayList<String> updated = new ArrayList<>(disabledBlockIds);
        updated.remove(normalized);
        if (!enabled) updated.add(normalized);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, updated, closingSlides,
                replaceContentEnabled(contentSlides, normalized, enabled),
                secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withClosingSlide(DocumentStudyClosingSlide slide) {
        ArrayList<DocumentStudyClosingSlide> updated = new ArrayList<>(closingSlides);
        updated.removeIf(item -> item.id().equals(slide.id()));
        updated.add(slide);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, disabledBlockIds, updated,
                contentSlides, secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withoutClosingSlide(String id) {
        ArrayList<DocumentStudyClosingSlide> updated = new ArrayList<>(closingSlides);
        updated.removeIf(item -> item.id().equals(id));
        ArrayList<String> enabledIds = new ArrayList<>(disabledBlockIds);
        enabledIds.remove(id);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, enabledIds, updated,
                contentSlides, secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    public DocumentStudyVideoConfiguration withContent(DocumentVideoSlideConfiguration slide) {
        ArrayList<DocumentVideoSlideConfiguration> updated = new ArrayList<>(contentSlides);
        updated.removeIf(item -> item.contentId().equals(slide.contentId()));
        updated.add(slide);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, disabledBlockIds,
                closingSlides, updated, secondarySlideInclusionMode, aiIllustrationsEnabled, aiIllustrationAppearance);
    }

    private static List<DocumentVideoSlideConfiguration> replaceContentVisual(
            List<DocumentVideoSlideConfiguration> current,
            DocumentParagraphVisualAssignment visual) {
        DocumentVideoSlideConfiguration previous = current.stream()
                .filter(item -> item.contentId().equals(visual.blockId())).findFirst()
                .orElseGet(() -> DocumentVideoSlideConfiguration.empty(visual.blockId()));
        return replaced(current, previous.withVisual(visual));
    }

    private static List<DocumentVideoSlideConfiguration> replaceContentDuration(
            List<DocumentVideoSlideConfiguration> current, String contentId, double duration) {
        DocumentVideoSlideConfiguration previous = current.stream()
                .filter(item -> item.contentId().equals(contentId)).findFirst()
                .orElseGet(() -> DocumentVideoSlideConfiguration.empty(contentId));
        return replaced(current, previous.withDuration(duration));
    }

    private static List<DocumentVideoSlideConfiguration> replaceContentEnabled(
            List<DocumentVideoSlideConfiguration> current, String contentId, boolean enabled) {
        DocumentVideoSlideConfiguration previous = current.stream()
                .filter(item -> item.contentId().equals(contentId)).findFirst()
                .orElseGet(() -> DocumentVideoSlideConfiguration.empty(contentId));
        return replaced(current, previous.withEnabled(enabled));
    }

    private static List<DocumentVideoSlideConfiguration> replaced(
            List<DocumentVideoSlideConfiguration> current,
            DocumentVideoSlideConfiguration replacement) {
        ArrayList<DocumentVideoSlideConfiguration> updated = new ArrayList<>(current);
        updated.removeIf(item -> item.contentId().equals(replacement.contentId()));
        updated.add(replacement);
        return List.copyOf(updated);
    }
}
