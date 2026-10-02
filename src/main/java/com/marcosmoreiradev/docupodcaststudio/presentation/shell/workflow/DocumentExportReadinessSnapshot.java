package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvedDocumentProcessingSelection;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable, side-effect-free view of persisted documentary export readiness. */
public record DocumentExportReadinessSnapshot(
        String correlationId,
        ResolvedDocumentProcessingSelection selection,
        Optional<NarrationScriptDocument> effectiveNarration,
        List<String> missingTranslationSegmentIds,
        List<String> invalidTranslationSegmentIds,
        List<String> missingAudioSegmentIds,
        List<String> staleAudioSegmentIds,
        List<String> invalidAudioSegmentIds,
        List<String> missingCompositionSegmentIds,
        boolean compositionsReady) {

    public DocumentExportReadinessSnapshot {
        correlationId = Objects.requireNonNullElse(correlationId, "").strip();
        selection = Objects.requireNonNull(selection, "selection");
        effectiveNarration = effectiveNarration == null
                ? Optional.empty() : effectiveNarration;
        missingTranslationSegmentIds = copy(missingTranslationSegmentIds);
        invalidTranslationSegmentIds = copy(invalidTranslationSegmentIds);
        missingAudioSegmentIds = copy(missingAudioSegmentIds);
        staleAudioSegmentIds = copy(staleAudioSegmentIds);
        invalidAudioSegmentIds = copy(invalidAudioSegmentIds);
        missingCompositionSegmentIds = copy(missingCompositionSegmentIds);
    }

    public boolean translationsReady() {
        return effectiveNarration.isPresent()
                && missingTranslationSegmentIds.isEmpty()
                && invalidTranslationSegmentIds.isEmpty();
    }

    public boolean audioReady() {
        return translationsReady()
                && missingAudioSegmentIds.isEmpty()
                && staleAudioSegmentIds.isEmpty()
                && invalidAudioSegmentIds.isEmpty();
    }

    public boolean readyToRender() {
        return audioReady() && compositionsReady;
    }

    /** Audio is current; export only needs its cheap segment-level composition cache. */
    public boolean compositionOnlyPending() {
        return audioReady() && (!compositionsReady
                || !missingCompositionSegmentIds.isEmpty());
    }

    public ReadinessStage blockingStage() {
        if (!translationsReady()) return ReadinessStage.NARRATION;
        if (!audioReady()) return ReadinessStage.AUDIO;
        if (compositionOnlyPending()) return ReadinessStage.COMPOSITION;
        return ReadinessStage.READY;
    }

    public enum ReadinessStage { NARRATION, AUDIO, COMPOSITION, READY }

    public int missingDerivativeCount() {
        return (int) java.util.stream.Stream.of(
                        missingTranslationSegmentIds, invalidTranslationSegmentIds,
                        missingAudioSegmentIds, staleAudioSegmentIds,
                        invalidAudioSegmentIds,
                        missingCompositionSegmentIds)
                .flatMap(List::stream).distinct().count();
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().toList();
    }
}
