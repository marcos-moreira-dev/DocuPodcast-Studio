package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.List;
import java.util.Optional;

/** Versioned textual sidecar for one page; PreparedPdfPage V3 remains authoritative fallback. */
public record PdfPageMap(
        int schemaVersion,
        int pageNumber,
        long sourcePageRevision,
        String builderSignature,
        PdfPageGeometry pageGeometry,
        List<PdfPageNode> nodes,
        List<PdfNarrationBinding> narrationBindings
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PdfPageMap {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || pageNumber <= 0 || sourcePageRevision <= 0
                || builderSignature == null || builderSignature.isBlank() || pageGeometry == null) {
            throw new IllegalArgumentException("Invalid PdfPageMap header");
        }
        builderSignature = builderSignature.strip();
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        narrationBindings = narrationBindings == null ? List.of() : List.copyOf(narrationBindings);
    }

    public Optional<PdfPageNode> node(String id) {
        return nodes.stream().filter(node -> node.id().equals(id)).findFirst();
    }

    public List<PdfPageNode> nodes(PdfPageNodeKind kind) {
        return nodes.stream().filter(node -> node.kind() == kind).toList();
    }
}
