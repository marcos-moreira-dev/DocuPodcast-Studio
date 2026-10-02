package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageGeometry;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMap;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNode;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNodeKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReviewState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/** Persists a human bbox/type/decision without rewriting V3 source evidence. */
public final class UpdatePdfVisualObjectUseCase {
    private final PdfPageMapRepository repository;

    public UpdatePdfVisualObjectUseCase(PdfPageMapRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public PdfPageNode update(PreparedPdfWorkspaceRef workspace, int pageNumber, String objectId,
                              PdfPageGeometry geometry, PdfVisualObjectProposal.Type type,
                              Decision decision) throws IOException {
        PdfPageMap pageMap = repository.loadPage(workspace.projectRoot(), pageNumber)
                .orElseThrow(() -> new IOException("No existe PdfPageMap para editar."));
        PdfPageNode current = pageMap.node(objectId)
                .filter(node -> node.kind() == PdfPageNodeKind.VISUAL_OBJECT)
                .orElseThrow(() -> new IOException("No existe el objeto visual " + objectId));
        PdfPageNode updated = new PdfPageNode(current.id(), current.legacyRegionId(), current.parentId(),
                current.kind(), type == null ? current.semanticType() : type.name(), current.order(),
                geometry == null ? current.geometry() : geometry,
                geometry == null ? current.geometryParts() : java.util.List.of(geometry),
                current.text(), current.evidence(), new PdfReviewState(true,
                Objects.requireNonNullElse(decision, Decision.CONFIRMED).name()), current.childIds());
        ArrayList<PdfPageNode> nodes = new ArrayList<>(pageMap.nodes());
        nodes.replaceAll(node -> node.id().equals(objectId) ? updated : node);
        repository.savePage(workspace.projectRoot(), workspace.sourceSha256(),
                new PdfPageMap(pageMap.schemaVersion(), pageMap.pageNumber(), pageMap.sourcePageRevision(),
                        pageMap.builderSignature(), pageMap.pageGeometry(), nodes, pageMap.narrationBindings()));
        return updated;
    }

    public enum Decision { CONFIRMED, REJECTED }
}
