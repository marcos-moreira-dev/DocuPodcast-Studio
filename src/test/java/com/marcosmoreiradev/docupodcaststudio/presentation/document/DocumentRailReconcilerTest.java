package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentRailReconcilerTest {
    @Test
    void identicalProjectionDoesNotMutateTheRail() {
        DocumentFragmentRailPresentation first = fragment("U001", "file:///one.png");
        DocumentFragmentRailPresentation second = fragment("U002", "file:///two.png");
        ObservableList<DocumentFragmentRailPresentation> target = FXCollections.observableArrayList(first, second);
        AtomicInteger changes = observe(target);

        DocumentRailReconciler.Result result = DocumentRailReconciler.reconcile(target, List.of(first, second));

        assertTrue(result.unchanged());
        assertEquals(0, changes.get());
        assertSame(first, target.get(0));
        assertSame(second, target.get(1));
    }

    @Test
    void mediaRevisionReplacesOnlyTheChangedUnit() {
        DocumentFragmentRailPresentation first = fragment("U001", "file:///one.png");
        DocumentFragmentRailPresentation second = fragment("U002", "file:///two.png");
        DocumentFragmentRailPresentation changed = fragment("U001", "file:///replacement.png");
        ObservableList<DocumentFragmentRailPresentation> target = FXCollections.observableArrayList(first, second);
        AtomicInteger changes = observe(target);

        DocumentRailReconciler.Result result = DocumentRailReconciler.reconcile(target, List.of(changed, second));

        assertFalse(result.structuralChange());
        assertEquals(1, result.replacements());
        assertEquals(1, changes.get());
        assertSame(changed, target.get(0));
        assertSame(second, target.get(1));
    }

    @Test
    void structuralChangesCanReplaceTheProjection() {
        ObservableList<DocumentFragmentRailPresentation> target = FXCollections.observableArrayList(
                fragment("U001", "file:///one.png"));

        DocumentRailReconciler.Result result = DocumentRailReconciler.reconcile(target, List.of(
                fragment("U001", "file:///one.png"), fragment("U002", "file:///two.png")));

        assertTrue(result.structuralChange());
        assertEquals(2, target.size());
    }

    private static AtomicInteger observe(ObservableList<DocumentFragmentRailPresentation> target) {
        AtomicInteger changes = new AtomicInteger();
        target.addListener((ListChangeListener<DocumentFragmentRailPresentation>) change -> {
            while (change.next()) {
                changes.incrementAndGet();
            }
        });
        return changes;
    }

    private static DocumentFragmentRailPresentation fragment(String unitId, String uri) {
        return new DocumentFragmentRailPresentation(
                unitId, "SEG-001", "B001", 0, 8, unitId, "Texto", "asset-" + unitId, uri);
    }
}
