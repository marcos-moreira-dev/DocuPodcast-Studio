package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Stable project-side anchor for user assignments over imported source text.
 *
 * <p>T93 kept the existing {@link DocumentTextRange} for compatibility. TI5 makes the
 * anchor operational: every new selection can carry selected text, hashes and context,
 * and a refresh/reconciliation pass can mark it as current, relocated, orphaned or in
 * need of review without editing the original Word/DOCX/PDF/TXT/Markdown source.</p>
 */
public record TextAnchor(
        String id,
        DocumentTextRange range,
        String selectedText,
        String selectedTextHash,
        String contextBefore,
        String contextAfter,
        String sourceSnapshotHash,
        TextAnchorConfidence confidence,
        TextAnchorStatus status
) {
    public TextAnchor {
        id = token(id, "id");
        range = Objects.requireNonNull(range, "range");
        selectedText = normalize(selectedText);
        selectedTextHash = normalize(selectedTextHash);
        contextBefore = normalize(contextBefore);
        contextAfter = normalize(contextAfter);
        sourceSnapshotHash = normalize(sourceSnapshotHash);
        confidence = confidence == null ? TextAnchorConfidence.LOW : confidence;
        status = status == null ? TextAnchorStatus.NEEDS_REVIEW : status;
        if (selectedTextHash.isBlank() && !selectedText.isBlank()) {
            selectedTextHash = sha256(selectedText);
        }
    }

    public static TextAnchor legacy(String assignmentId, DocumentTextRange range) {
        if (range == null) {
            return null;
        }
        return new TextAnchor(
                "ANCH-" + safeId(assignmentId),
                range,
                "",
                "",
                "",
                "",
                "",
                TextAnchorConfidence.LOW,
                TextAnchorStatus.NEEDS_REVIEW
        );
    }

    public static TextAnchor fromSelection(String id,
                                           DocumentTextRange range,
                                           String selectedText,
                                           String contextBefore,
                                           String contextAfter,
                                           String sourceSnapshotHash) {
        return new TextAnchor(
                id,
                range,
                selectedText,
                "",
                contextBefore,
                contextAfter,
                sourceSnapshotHash,
                selectedText == null || selectedText.isBlank() ? TextAnchorConfidence.MEDIUM : TextAnchorConfidence.HIGH,
                TextAnchorStatus.CURRENT
        );
    }

    /** Builds a rich anchor directly from a document block selection. */
    public static TextAnchor fromDocumentSelection(String id,
                                                   ReadableDocument document,
                                                   DocumentTextRange range,
                                                   String sourceSnapshotHash) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(range, "range");
        DocumentBlock block = document.blockById(range.blockId())
                .orElseThrow(() -> new IllegalArgumentException("No existe el bloque " + range.blockId()));
        String text = block.text();
        int start = Math.min(Math.max(0, range.startOffset()), text.length());
        int end = Math.min(Math.max(start, range.endOffset()), text.length());
        String selected = text.substring(start, end);
        String before = text.substring(Math.max(0, start - 80), start);
        String after = text.substring(end, Math.min(text.length(), end + 80));
        return fromSelection(id, new DocumentTextRange(range.blockId(), start, end), selected, before, after, sourceSnapshotHash);
    }

    public TextAnchor withStatus(TextAnchorStatus nextStatus, TextAnchorConfidence nextConfidence) {
        return new TextAnchor(id, range, selectedText, selectedTextHash, contextBefore, contextAfter,
                sourceSnapshotHash, nextConfidence, nextStatus);
    }

    public TextAnchor relocated(DocumentTextRange nextRange, TextAnchorConfidence nextConfidence, String nextSnapshotHash) {
        return new TextAnchor(id, nextRange, selectedText, selectedTextHash, contextBefore, contextAfter,
                normalize(nextSnapshotHash).isBlank() ? sourceSnapshotHash : nextSnapshotHash,
                nextConfidence == null ? TextAnchorConfidence.MEDIUM : nextConfidence,
                TextAnchorStatus.RELOCATED);
    }

    public TextAnchor current(DocumentTextRange confirmedRange, String nextSnapshotHash) {
        return new TextAnchor(id,
                confirmedRange == null ? range : confirmedRange,
                selectedText,
                selectedTextHash,
                contextBefore,
                contextAfter,
                normalize(nextSnapshotHash).isBlank() ? sourceSnapshotHash : nextSnapshotHash,
                TextAnchorConfidence.HIGH,
                TextAnchorStatus.CURRENT);
    }

    public TextAnchor needsReview() {
        return withStatus(TextAnchorStatus.NEEDS_REVIEW, TextAnchorConfidence.MEDIUM);
    }

    public TextAnchor orphaned(String nextSnapshotHash) {
        return new TextAnchor(id, range, selectedText, selectedTextHash, contextBefore, contextAfter,
                normalize(nextSnapshotHash).isBlank() ? sourceSnapshotHash : nextSnapshotHash,
                TextAnchorConfidence.LOW,
                TextAnchorStatus.ORPHANED);
    }

    public boolean hasSelectedText() {
        return !selectedText.isBlank();
    }

    public boolean current() {
        return status == TextAnchorStatus.CURRENT;
    }

    public boolean hashMatches(String text) {
        return !selectedTextHash.isBlank() && selectedTextHash.equals(sha256(text));
    }

    public String displayLabel() {
        String text = selectedText.isBlank() ? "sin texto validado" : selectedText;
        if (text.length() > 48) {
            text = text.substring(0, 45) + "…";
        }
        return range.displayLabel() + " · " + status.name() + " · " + confidence.name() + " · “" + text + "”";
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(normalize(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
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

    private static String safeId(String value) {
        String normalized = normalize(value).replaceAll("[^A-Za-z0-9_-]", "-");
        return normalized.isBlank() ? "LEGACY" : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
