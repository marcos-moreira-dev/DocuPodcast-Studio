package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.util.Optional;

/**
 * Coordinates document-selection labels and primary document action copy.
 *
 * <p>The shell view-model owns JavaFX properties; this coordinator owns the
 * presentation rules for selected blocks/ranges so the same labels are reused
 * by the reader, side panels and status bar without duplicating text logic.</p>
 */
public final class DocumentSelectionCoordinator {
    private static final int PREVIEW_LIMIT = 90;

    public String normalizeBlockId(String blockId) {
        return blockId == null ? "" : blockId.strip();
    }

    public SelectionLabels cleared(String selectedBlockId, ReadableDocument document) {
        String normalized = normalizeBlockId(selectedBlockId);
        String label = normalized.isBlank() ? "Sin texto seleccionado" : "Bloque seleccionado: " + normalized;
        return new SelectionLabels("", label, sourceLocationLabel(document, normalized));
    }

    public SelectionLabels sentenceSelection(DocumentTextRange range, String selectedText, ReadableDocument document) {
        String preview = previewText(selectedText);
        return new SelectionLabels(
                preview,
                "Oración seleccionada: “" + preview + "”",
                sourceLocationLabel(document, range == null ? "" : range.blockId()));
    }

    public SelectionLabels layerSelection(String displayName, DocumentTextRange range, ReadableDocument document) {
        String preview = previewText(displayName);
        return new SelectionLabels(
                preview,
                "Capa seleccionada: " + preview,
                sourceLocationLabel(document, range == null ? "" : range.blockId()));
    }

    public SelectionLabels documentRangeSelection(ReadableDocument document, DocumentTextRange range, String fallbackPreview,
                                                  String labelPrefix) {
        String preview = previewForRange(document, range).orElse(previewText(fallbackPreview));
        String prefix = labelPrefix == null || labelPrefix.isBlank() ? "Oración seleccionada" : labelPrefix.strip();
        return new SelectionLabels(
                preview,
                prefix + ": “" + preview + "”",
                sourceLocationLabel(document, range == null ? "" : range.blockId()));
    }

    public SelectionLabels blockSelection(ReadableDocument document, String blockId) {
        String normalized = normalizeBlockId(blockId);
        Optional<DocumentBlock> block = document == null ? Optional.empty() : document.blockById(normalized);
        String preview = block.map(value -> previewText(value.text())).orElse("");
        String label = preview.isBlank()
                ? "Bloque seleccionado: " + normalized
                : "Fragmento seleccionado: “" + preview + "”";
        return new SelectionLabels(preview, label, sourceLocationLabel(document, normalized));
    }

    public PrimaryAction primaryAction(ReadableDocument document, DocumentTextRange selectedRange, String selectedBlockId) {
        if (document == null) {
            return new PrimaryAction(
                    "Escuchar documento",
                    "Abre un Word/DOCX para iniciar lectura narrada.");
        }
        if (selectedRange != null) {
            return new PrimaryAction(
                    "Reproducir desde selección",
                    "Reproducir selección como punto de inicio: continúa con las siguientes oraciones si sus WAV ya existen o espera buffer si se están generando.");
        }
        if (!normalizeBlockId(selectedBlockId).isBlank()) {
            return new PrimaryAction(
                    "Reproducir desde aquí",
                    "Usa el bloque seleccionado como punto de inicio; si falta lectura preparada o audio, DocuPodcast prepara la lectura y continúa desde ahí.");
        }
        return new PrimaryAction(
                "Reproducir documento",
                "Una acción principal: prepara la lectura, genera audio si hace falta o reproduce cuando ya exista manifest.");
    }

    public Optional<String> previewForRange(ReadableDocument document, DocumentTextRange range) {
        if (document == null || range == null) {
            return Optional.empty();
        }
        return document.blockById(range.blockId())
                .map(block -> {
                    int start = Math.max(0, Math.min(range.startOffset(), block.text().length()));
                    int end = Math.max(start, Math.min(range.endOffset(), block.text().length()));
                    return previewText(block.text().substring(start, end));
                })
                .filter(text -> !text.isBlank());
    }

    public String sourceLocationLabel(ReadableDocument document, String blockId) {
        String normalized = normalizeBlockId(blockId);
        if (document == null || normalized.isBlank()) {
            return "Ubicación fuente: sin selección.";
        }
        return document.blockById(normalized)
                .map(value -> "Ubicación fuente: " + value.sourceLocatorLabel(document.format()))
                .orElse("Ubicación fuente: bloque " + normalized);
    }

    public String previewText(String text) {
        String normalized = text == null ? "" : text.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, PREVIEW_LIMIT)).strip() + "…";
    }

    public record SelectionLabels(String preview, String rangeLabel, String sourceLocation) {
    }

    public record PrimaryAction(String label, String hint) {
    }
}
