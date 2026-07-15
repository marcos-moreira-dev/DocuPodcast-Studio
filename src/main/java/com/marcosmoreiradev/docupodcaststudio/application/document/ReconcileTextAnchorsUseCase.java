package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchorConfidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchorStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reconciles narrative layer text anchors after the read-only source document is refreshed.
 *
 * <p>TI5 keeps the Word/PDF/TXT/Markdown source immutable. The project-side layer may be
 * confirmed, relocated, marked as needing review, or orphaned. This is intentionally
 * deterministic and local: no OCR, no network, no LLM.</p>
 */
public final class ReconcileTextAnchorsUseCase {
    public TextAnchorReconciliationReport reconcile(
            List<NarrativeLayerAssignment> assignments,
            ReadableDocument refreshedDocument,
            String sourceSnapshotHash
    ) {
        Objects.requireNonNull(refreshedDocument, "refreshedDocument");
        List<NarrativeLayerAssignment> safeAssignments = assignments == null ? List.of() : assignments;
        ArrayList<TextAnchorReconciliationEntry> entries = new ArrayList<>();
        for (NarrativeLayerAssignment assignment : safeAssignments) {
            TextAnchor previous = assignment.textAnchor();
            if (previous == null) {
                TextAnchor legacy = TextAnchor.legacy(assignment.id(), assignment.documentRange());
                NarrativeLayerAssignment updated = assignment.withTextAnchor(legacy);
                entries.add(new TextAnchorReconciliationEntry(
                        assignment.id(), previous, legacy, updated, "Sin TextAnchor rico; se conserva como legacy/needs-review."));
                continue;
            }
            ReconciliationDecision decision = reconcileAnchor(previous, refreshedDocument, sourceSnapshotHash);
            NarrativeLayerAssignment updated = assignment.withTextAnchor(decision.anchor());
            entries.add(new TextAnchorReconciliationEntry(
                    assignment.id(), previous, decision.anchor(), updated, decision.reason()));
        }
        return new TextAnchorReconciliationReport(entries);
    }

    private static ReconciliationDecision reconcileAnchor(TextAnchor anchor, ReadableDocument document, String snapshotHash) {
        if (!anchor.hasSelectedText()) {
            return new ReconciliationDecision(anchor.withStatus(TextAnchorStatus.NEEDS_REVIEW, TextAnchorConfidence.LOW),
                    "Anchor legacy sin texto seleccionado: requiere revisión manual.");
        }
        String selected = anchor.selectedText();
        DocumentTextRange sameRange = confirmedSameRange(anchor, document);
        if (sameRange != null) {
            return new ReconciliationDecision(anchor.current(sameRange, snapshotHash),
                    "El texto seleccionado sigue en el mismo rango.");
        }
        List<DocumentTextRange> matches = exactMatches(selected, document);
        if (matches.size() == 1) {
            // Source guardrail: relocation produces TextAnchorStatus.RELOCATED through TextAnchor.relocated(...).
            return new ReconciliationDecision(anchor.relocated(matches.get(0), TextAnchorConfidence.HIGH, snapshotHash),
                    "El texto seleccionado se encontró una sola vez en otro rango.");
        }
        if (matches.size() > 1) {
            return new ReconciliationDecision(anchor.withStatus(TextAnchorStatus.NEEDS_REVIEW, TextAnchorConfidence.MEDIUM),
                    "El texto seleccionado aparece varias veces; requiere revisión para evitar falsa reubicación.");
        }
        // Source guardrail: missing text produces TextAnchorStatus.ORPHANED through TextAnchor.orphaned(...).
        return new ReconciliationDecision(anchor.orphaned(snapshotHash),
                "El texto seleccionado ya no se encontró en el documento refrescado.");
    }

    private static DocumentTextRange confirmedSameRange(TextAnchor anchor, ReadableDocument document) {
        return document.blockById(anchor.range().blockId())
                .flatMap(block -> textAtRange(block, anchor.range()))
                .filter(anchor::hashMatches)
                .map(text -> anchor.range())
                .orElse(null);
    }

    private static java.util.Optional<String> textAtRange(DocumentBlock block, DocumentTextRange range) {
        String text = block.text();
        if (range.startOffset() > text.length() || range.endOffset() > text.length()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(text.substring(range.startOffset(), range.endOffset()));
    }

    private static List<DocumentTextRange> exactMatches(String selectedText, ReadableDocument document) {
        if (selectedText == null || selectedText.isBlank()) {
            return List.of();
        }
        ArrayList<DocumentTextRange> matches = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            String text = block.text();
            int from = 0;
            while (from <= text.length()) {
                int idx = text.indexOf(selectedText, from);
                if (idx < 0) {
                    break;
                }
                matches.add(new DocumentTextRange(block.id(), idx, idx + selectedText.length()));
                from = idx + Math.max(1, selectedText.length());
            }
        }
        return List.copyOf(matches);
    }

    private record ReconciliationDecision(TextAnchor anchor, String reason) {
    }
}
