package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/** Resolves one unified selection without leaking PDF regions as document blocks. */
public final class ResolveDocumentSelectionUseCase {
    private final PreparedPdfDocumentRepository repository;

    public ResolveDocumentSelectionUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<DocumentSelectionSnapshot> resolve(ProjectDocumentSource source,
                                                       DocumentSelectionRef selection) {
        if (source instanceof BlockDocumentSource blockSource
                && selection instanceof BlockSelectionRef blockSelection) {
            return blockSource.document().blockById(blockSelection.blockId())
                    .map(block -> blockSnapshot(block, blockSelection));
        }
        if (source instanceof PreparedPdfSource pdfSource
                && selection instanceof PdfRegionSelectionRef pdfSelection) {
            try {
                return repository.loadPage(pdfSource.workspace().projectRoot(), pdfSelection.pageNumber())
                        .flatMap(page -> page.regions().stream()
                                .filter(region -> region.id().equals(pdfSelection.regionId()))
                                .findFirst()
                                .map(region -> {
                                    String text = slice(region.effectiveText(),
                                            pdfSelection.startOffset(), pdfSelection.endOffset());
                                    return new DocumentSelectionSnapshot(pdfSelection, text,
                                            region.evidence().origin().name(),
                                            region.effectiveType().name(), page.pageNumber(),
                                            new PdfPageRegion(page.pageNumber(), region.xMin(), region.yMin(),
                                                    region.xMax(), region.yMax(),
                                                    page.widthPoints(), page.heightPoints()),
                                            region.revision(), region.effectiveNarratability());
                                }));
            } catch (IOException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static DocumentSelectionSnapshot blockSnapshot(DocumentBlock block,
                                                            BlockSelectionRef selection) {
        return new DocumentSelectionSnapshot(selection,
                slice(block.text(), selection.startOffset(), selection.endOffset()),
                block.originalStyle(), block.type().name(), 0, null, 0,
                block.type().narratableByDefault() ? PdfNarratability.NARRATABLE
                        : PdfNarratability.NON_NARRATABLE);
    }

    private static String slice(String text, int start, int end) {
        String safe = text == null ? "" : text;
        int from = Math.min(Math.max(0, start), safe.length());
        int to = end <= from ? safe.length() : Math.min(end, safe.length());
        return safe.substring(from, to);
    }
}
