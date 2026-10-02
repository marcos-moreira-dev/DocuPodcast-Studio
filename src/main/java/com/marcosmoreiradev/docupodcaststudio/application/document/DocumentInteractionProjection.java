package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;

import java.util.Objects;
import java.util.Set;

/**
 * Source-neutral document interaction consumed by shell controls and audio orchestration.
 * Canonical Word and PDF models stay behind their respective adapters.
 */
public record DocumentInteractionProjection(
        SourceKind sourceKind,
        boolean selectionValid,
        String preferredNarrationSegmentId,
        DocumentProcessingScope effectiveScope,
        boolean narrationAvailable,
        boolean audioCoverageAvailable,
        Set<DocumentProcessingScope> supportedScopes
) {
    public enum SourceKind { NONE, WORD, PDF }

    public DocumentInteractionProjection {
        sourceKind = Objects.requireNonNullElse(sourceKind, SourceKind.NONE);
        preferredNarrationSegmentId = Objects.toString(
                preferredNarrationSegmentId, "").strip();
        effectiveScope = Objects.requireNonNullElse(
                effectiveScope, DocumentProcessingScope.FULL_DOCUMENT);
        supportedScopes = supportedScopes == null || supportedScopes.isEmpty()
                ? Set.of(DocumentProcessingScope.FULL_DOCUMENT)
                : Set.copyOf(supportedScopes);
        if (!supportedScopes.contains(DocumentProcessingScope.FULL_DOCUMENT)) {
            throw new IllegalArgumentException("FULL_DOCUMENT must always be supported");
        }
        if (!supportedScopes.contains(effectiveScope)
                || (!selectionValid && partial(effectiveScope))) {
            effectiveScope = DocumentProcessingScope.FULL_DOCUMENT;
        }
    }

    /** Compatibility constructor for callers that do not yet expose source capabilities. */
    public DocumentInteractionProjection(
            SourceKind sourceKind,
            boolean selectionValid,
            String preferredNarrationSegmentId,
            DocumentProcessingScope effectiveScope,
            boolean narrationAvailable,
            boolean audioCoverageAvailable) {
        this(sourceKind, selectionValid, preferredNarrationSegmentId, effectiveScope,
                narrationAvailable, audioCoverageAvailable,
                sourceKind == SourceKind.PDF
                        ? Set.of(DocumentProcessingScope.values())
                        : sourceKind == SourceKind.WORD
                        ? Set.of(DocumentProcessingScope.FULL_DOCUMENT,
                        DocumentProcessingScope.FROM_SELECTION,
                        DocumentProcessingScope.SINGLE_FRAGMENT,
                        DocumentProcessingScope.INTERVAL)
                        : Set.of(DocumentProcessingScope.FULL_DOCUMENT));
    }

    public static DocumentInteractionProjection none() {
        return new DocumentInteractionProjection(SourceKind.NONE, false, "",
                DocumentProcessingScope.FULL_DOCUMENT, false, false);
    }

    public boolean partialScopeAvailable() {
        return selectionValid;
    }

    public boolean supports(DocumentProcessingScope scope) {
        return scope != null && supportedScopes.contains(scope);
    }

    public boolean selectable(DocumentProcessingScope scope) {
        return supports(scope) && (!partial(scope) || selectionValid);
    }

    public static boolean partial(DocumentProcessingScope scope) {
        return scope == DocumentProcessingScope.FROM_SELECTION
                || scope == DocumentProcessingScope.SINGLE_FRAGMENT;
    }
}
