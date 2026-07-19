package com.marcosmoreiradev.docupodcaststudio.domain.narrative;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Project-side narrative production state, independent from documentary and theatre data. */
public record NarrativeProjectLayer(
        NarrativeVideoConfiguration videoConfiguration,
        String documentFingerprint,
        String normalizedDocumentText,
        List<NarrativeContextReference> contextReferences,
        List<NarrativeParagraphTake> paragraphTakes
) {
    public NarrativeProjectLayer {
        videoConfiguration = Objects.requireNonNullElseGet(
                videoConfiguration, NarrativeVideoConfiguration::verticalDefaults);
        documentFingerprint = optional(documentFingerprint);
        normalizedDocumentText = optional(normalizedDocumentText);
        contextReferences = contextReferences == null ? List.of() : contextReferences.stream()
                .filter(Objects::nonNull).toList();
        paragraphTakes = paragraphTakes == null ? List.of() : paragraphTakes.stream()
                .filter(Objects::nonNull).toList();
    }

    public static NarrativeProjectLayer empty() {
        return new NarrativeProjectLayer(
                NarrativeVideoConfiguration.verticalDefaults(), "", "", List.of(), List.of());
    }

    public Optional<NarrativeParagraphTake> take(String blockId) {
        String normalized = optional(blockId);
        return paragraphTakes.stream().filter(item -> item.blockId().equals(normalized)).findFirst();
    }

    public NarrativeParagraphTake takeOrDefault(String blockId) {
        return take(blockId).orElseGet(() -> NarrativeParagraphTake.empty(blockId));
    }

    public NarrativeProjectLayer withVideoConfiguration(NarrativeVideoConfiguration value) {
        return new NarrativeProjectLayer(value, documentFingerprint, normalizedDocumentText,
                contextReferences, markTakesStale(paragraphTakes));
    }

    public NarrativeProjectLayer withDocumentSnapshot(String fingerprint, String text) {
        String normalizedFingerprint = optional(fingerprint);
        boolean changed = !normalizedFingerprint.equals(documentFingerprint);
        return new NarrativeProjectLayer(videoConfiguration, normalizedFingerprint, text,
                contextReferences, changed ? markTakesStale(paragraphTakes) : paragraphTakes);
    }

    public NarrativeProjectLayer withContextReference(NarrativeContextReference reference) {
        Objects.requireNonNull(reference, "reference");
        ArrayList<NarrativeContextReference> updated = new ArrayList<>(contextReferences);
        updated.removeIf(item -> item.id().equals(reference.id()));
        updated.add(reference);
        return new NarrativeProjectLayer(videoConfiguration, documentFingerprint, normalizedDocumentText,
                updated, markTakesStale(paragraphTakes));
    }

    public NarrativeProjectLayer withoutContextReference(String id) {
        String normalized = optional(id);
        List<NarrativeContextReference> updated = contextReferences.stream()
                .filter(item -> !item.id().equals(normalized)).toList();
        return new NarrativeProjectLayer(videoConfiguration, documentFingerprint, normalizedDocumentText,
                updated, markTakesStale(paragraphTakes));
    }

    public NarrativeProjectLayer withTake(NarrativeParagraphTake take) {
        Objects.requireNonNull(take, "take");
        ArrayList<NarrativeParagraphTake> updated = new ArrayList<>(paragraphTakes);
        updated.removeIf(item -> item.blockId().equals(take.blockId()));
        updated.add(take);
        return new NarrativeProjectLayer(videoConfiguration, documentFingerprint, normalizedDocumentText,
                contextReferences, updated);
    }

    private static List<NarrativeParagraphTake> markTakesStale(List<NarrativeParagraphTake> takes) {
        return takes.stream().map(NarrativeParagraphTake::markStale).toList();
    }

    private static String optional(String value) {
        return value == null ? "" : value.strip();
    }
}
