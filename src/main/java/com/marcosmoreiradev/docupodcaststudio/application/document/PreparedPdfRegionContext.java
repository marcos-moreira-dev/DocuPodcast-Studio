package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Selected PDF region plus its canonical reading-order neighborhood. */
public record PreparedPdfRegionContext(
        DocumentSelectionSnapshot selected,
        List<DocumentSelectionSnapshot> before,
        List<DocumentSelectionSnapshot> after
) {
    public PreparedPdfRegionContext {
        selected = java.util.Objects.requireNonNull(selected, "selected");
        before = before == null ? List.of() : List.copyOf(before);
        after = after == null ? List.of() : List.copyOf(after);
    }
}
