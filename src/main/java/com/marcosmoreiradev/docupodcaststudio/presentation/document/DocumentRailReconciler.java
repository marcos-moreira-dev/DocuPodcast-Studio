package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;

/** Applies the smallest possible change set to the virtual visual-fragment rail. */
public final class DocumentRailReconciler {
    private DocumentRailReconciler() {}

    public static Result reconcile(
            ObservableList<DocumentFragmentRailPresentation> target,
            List<DocumentFragmentRailPresentation> requested) {
        Objects.requireNonNull(target, "target");
        List<DocumentFragmentRailPresentation> next = requested == null ? List.of() : List.copyOf(requested);

        if (!sameStructure(target, next)) {
            target.setAll(next);
            return new Result(true, next.size());
        }

        int replacements = 0;
        for (int index = 0; index < next.size(); index++) {
            DocumentFragmentRailPresentation current = target.get(index);
            DocumentFragmentRailPresentation replacement = next.get(index);
            if (!current.equals(replacement)) {
                target.set(index, replacement);
                replacements++;
            }
        }
        return new Result(false, replacements);
    }

    private static boolean sameStructure(
            List<DocumentFragmentRailPresentation> current,
            List<DocumentFragmentRailPresentation> next) {
        if (current.size() != next.size()) {
            return false;
        }
        for (int index = 0; index < current.size(); index++) {
            if (!current.get(index).unitId().equals(next.get(index).unitId())) {
                return false;
            }
        }
        return true;
    }

    public record Result(boolean structuralChange, int replacements) {
        public boolean unchanged() {
            return !structuralChange && replacements == 0;
        }
    }
}
