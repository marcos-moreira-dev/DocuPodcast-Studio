package com.marcosmoreiradev.docupodcaststudio.domain.assignment;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

/**
 * Production annotation over a narration range: voice, user audio, emotion, image,
 * ambient sound or note. It never modifies the imported Word/DOCX text.
 */
public record NarrativeLayerAssignment(
        String id,
        NarrativeLayerKind kind,
        ScriptTextRange textRange,
        DocumentTextRange documentRange,
        TextAnchor textAnchor,
        String targetId,
        String displayName,
        String notes
) {
    public NarrativeLayerAssignment(
            String id,
            NarrativeLayerKind kind,
            ScriptTextRange textRange,
            String targetId,
            String displayName,
            String notes) {
        this(id, kind, textRange, null, null, targetId, displayName, notes);
    }

    public NarrativeLayerAssignment(
            String id,
            NarrativeLayerKind kind,
            ScriptTextRange textRange,
            DocumentTextRange documentRange,
            String targetId,
            String displayName,
            String notes) {
        this(id, kind, textRange, documentRange, null, targetId, displayName, notes);
    }

    public NarrativeLayerAssignment {
        id = token(id, "id");
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
        if (textRange == null) {
            throw new IllegalArgumentException("textRange is required");
        }
        if (documentRange == null && textAnchor != null) {
            documentRange = textAnchor.range();
        }
        if (textAnchor == null && documentRange != null) {
            textAnchor = TextAnchor.legacy(id, documentRange);
        }
        targetId = normalize(targetId);
        displayName = normalize(displayName);
        notes = normalize(notes);
        if (!kind.equals(NarrativeLayerKind.NOTE) && targetId.isBlank()) {
            throw new IllegalArgumentException("targetId is required for " + kind.name());
        }
    }


    public NarrativeLayerAssignment withTextAnchor(TextAnchor nextAnchor) {
        if (nextAnchor == null) {
            return new NarrativeLayerAssignment(id, kind, textRange, documentRange, null, targetId, displayName, notes);
        }
        return new NarrativeLayerAssignment(id, kind, textRange, nextAnchor.range(), nextAnchor, targetId, displayName, notes);
    }

    public NarrativeLayerAssignment withDocumentRange(DocumentTextRange nextRange) {
        return new NarrativeLayerAssignment(id, kind, textRange, nextRange, textAnchor, targetId, displayName, notes);
    }

    public boolean overlaps(NarrativeLayerAssignment other) {
        if (other == null) {
            return false;
        }
        if (!textRange.segmentId().equals(other.textRange.segmentId())) {
            return false;
        }
        return textRange.startOffset() < other.textRange.endOffset()
                && other.textRange.startOffset() < textRange.endOffset();
    }

    public boolean primaryNarrationLayer() {
        return kind.primaryNarrationLayer();
    }

    public boolean hasDocumentRange() {
        return documentRange != null;
    }

    public boolean hasTextAnchor() {
        return textAnchor != null;
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
