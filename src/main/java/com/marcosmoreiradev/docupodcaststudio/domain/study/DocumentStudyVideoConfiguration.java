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
        List<DocumentStudyClosingSlide> closingSlides
) {
    public DocumentStudyVideoConfiguration {
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
    }

    public DocumentStudyVideoConfiguration(String videoTitle,
                                           double defaultTableDurationSeconds,
                                           List<DocumentParagraphVisualAssignment> paragraphVisuals,
                                           List<DocumentTableSlideConfiguration> tableSlides,
                                           List<DocumentStudyMusicTrack> musicTracks) {
        this(videoTitle, defaultTableDurationSeconds, paragraphVisuals, tableSlides, musicTracks,
                List.of(), List.of());
    }

    public DocumentStudyVideoConfiguration(String videoTitle,
                                           double defaultTableDurationSeconds,
                                           List<DocumentParagraphVisualAssignment> paragraphVisuals,
                                           List<DocumentTableSlideConfiguration> tableSlides,
                                           List<DocumentStudyMusicTrack> musicTracks,
                                           List<String> disabledBlockIds) {
        this(videoTitle, defaultTableDurationSeconds, paragraphVisuals, tableSlides, musicTracks,
                disabledBlockIds, List.of());
    }

    public static DocumentStudyVideoConfiguration empty() {
        return new DocumentStudyVideoConfiguration("", 6.0, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public Optional<DocumentParagraphVisualAssignment> paragraph(String blockId) {
        return paragraphVisuals.stream().filter(item -> item.blockId().equals(blockId)).findFirst();
    }

    public double tableDuration(String blockId) {
        return tableSlides.stream().filter(item -> item.blockId().equals(blockId)).findFirst()
                .map(DocumentTableSlideConfiguration::durationSeconds).orElse(defaultTableDurationSeconds);
    }

    public boolean blockEnabled(String blockId) {
        return blockId == null || !disabledBlockIds.contains(blockId.strip());
    }

    public Optional<DocumentStudyClosingSlide> closingSlide(String id) {
        return closingSlides.stream().filter(item -> item.id().equals(id)).findFirst();
    }

    public DocumentStudyVideoConfiguration withTitle(String title) {
        return new DocumentStudyVideoConfiguration(title, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, disabledBlockIds, closingSlides);
    }

    public DocumentStudyVideoConfiguration withDefaultTableDuration(double seconds) {
        return new DocumentStudyVideoConfiguration(videoTitle, seconds, paragraphVisuals, tableSlides, musicTracks,
                disabledBlockIds, closingSlides);
    }

    public DocumentStudyVideoConfiguration withParagraph(DocumentParagraphVisualAssignment assignment) {
        ArrayList<DocumentParagraphVisualAssignment> updated = new ArrayList<>(paragraphVisuals);
        updated.removeIf(item -> item.blockId().equals(assignment.blockId()));
        updated.add(assignment);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                updated, tableSlides, musicTracks, disabledBlockIds, closingSlides);
    }

    public DocumentStudyVideoConfiguration withTable(DocumentTableSlideConfiguration table) {
        ArrayList<DocumentTableSlideConfiguration> updated = new ArrayList<>(tableSlides);
        updated.removeIf(item -> item.blockId().equals(table.blockId()));
        updated.add(table);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, updated, musicTracks, disabledBlockIds, closingSlides);
    }

    public DocumentStudyVideoConfiguration withoutTable(String blockId) {
        ArrayList<DocumentTableSlideConfiguration> updated = new ArrayList<>(tableSlides);
        updated.removeIf(item -> item.blockId().equals(blockId));
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, updated, musicTracks, disabledBlockIds, closingSlides);
    }

    public DocumentStudyVideoConfiguration withMusicTracks(List<DocumentStudyMusicTrack> tracks) {
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, tracks, disabledBlockIds, closingSlides);
    }

    public DocumentStudyVideoConfiguration withBlockEnabled(String blockId, boolean enabled) {
        String normalized = blockId == null ? "" : blockId.strip();
        if (normalized.isBlank()) return this;
        ArrayList<String> updated = new ArrayList<>(disabledBlockIds);
        updated.remove(normalized);
        if (!enabled) updated.add(normalized);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, updated, closingSlides);
    }

    public DocumentStudyVideoConfiguration withClosingSlide(DocumentStudyClosingSlide slide) {
        ArrayList<DocumentStudyClosingSlide> updated = new ArrayList<>(closingSlides);
        updated.removeIf(item -> item.id().equals(slide.id()));
        updated.add(slide);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, disabledBlockIds, updated);
    }

    public DocumentStudyVideoConfiguration withoutClosingSlide(String id) {
        ArrayList<DocumentStudyClosingSlide> updated = new ArrayList<>(closingSlides);
        updated.removeIf(item -> item.id().equals(id));
        ArrayList<String> enabledIds = new ArrayList<>(disabledBlockIds);
        enabledIds.remove(id);
        return new DocumentStudyVideoConfiguration(videoTitle, defaultTableDurationSeconds,
                paragraphVisuals, tableSlides, musicTracks, enabledIds, updated);
    }
}
