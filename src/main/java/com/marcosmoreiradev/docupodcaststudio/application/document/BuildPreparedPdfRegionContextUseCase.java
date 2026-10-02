package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reads context around a selected region directly from the volatile V2 region index. */
public final class BuildPreparedPdfRegionContextUseCase {
    private final PreparedPdfDocumentRepository repository;

    public BuildPreparedPdfRegionContextUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<PreparedPdfRegionContext> build(PreparedPdfWorkspaceRef workspace,
                                                    PdfRegionSelectionRef selection,
                                                    int radius) throws IOException {
        if (workspace == null || selection == null) return Optional.empty();
        List<PreparedPdfRegionIndex.Entry> entries =
                repository.loadRegionIndex(workspace.projectRoot()).regions();
        int selectedIndex = -1;
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            if (entry.pageNumber() == selection.pageNumber()
                    && entry.region().id().equals(selection.regionId())) {
                selectedIndex = index;
                break;
            }
        }
        if (selectedIndex < 0) return Optional.empty();
        int window = Math.max(0, Math.min(radius, 25));
        ArrayList<DocumentSelectionSnapshot> before = new ArrayList<>();
        ArrayList<DocumentSelectionSnapshot> after = new ArrayList<>();
        for (int index = Math.max(0, selectedIndex - window); index < selectedIndex; index++) {
            before.add(snapshot(entries.get(index), whole(entries.get(index))));
        }
        for (int index = selectedIndex + 1;
             index <= Math.min(entries.size() - 1, selectedIndex + window); index++) {
            after.add(snapshot(entries.get(index), whole(entries.get(index))));
        }
        return Optional.of(new PreparedPdfRegionContext(
                snapshot(entries.get(selectedIndex), selection), before, after));
    }

    private static PdfRegionSelectionRef whole(PreparedPdfRegionIndex.Entry entry) {
        return new PdfRegionSelectionRef(entry.pageNumber(), entry.region().id(),
                0, entry.region().effectiveText().length());
    }

    private static DocumentSelectionSnapshot snapshot(PreparedPdfRegionIndex.Entry entry,
                                                      PdfRegionSelectionRef selection) {
        PdfRegion region = entry.region();
        int from = Math.min(selection.startOffset(), region.effectiveText().length());
        int to = selection.endOffset() <= from ? region.effectiveText().length()
                : Math.min(selection.endOffset(), region.effectiveText().length());
        return new DocumentSelectionSnapshot(selection,
                region.effectiveText().substring(from, to),
                region.evidence().origin().name(), region.effectiveType().name(),
                entry.pageNumber(),
                new PdfPageRegion(entry.pageNumber(), region.xMin(), region.yMin(),
                        region.xMax(), region.yMax(),
                        entry.pageWidthPoints(), entry.pageHeightPoints()),
                region.revision(), region.effectiveNarratability());
    }
}
