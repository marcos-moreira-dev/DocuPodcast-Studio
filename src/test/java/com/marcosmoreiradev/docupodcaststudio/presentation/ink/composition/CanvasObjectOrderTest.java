package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CanvasObjectOrderTest {
    @Test void preservesSelectedOrderAndMovesOnlyOneUnselectedNeighbour() {
        var source = List.of("a", "b", "c", "d", "e");
        var selected = Set.of("b", "c");
        assertEquals(List.of("a", "d", "b", "c", "e"), CanvasObjectOrder.reorder(source, selected, 1));
        assertEquals(List.of("b", "c", "a", "d", "e"), CanvasObjectOrder.reorder(source, selected, -1));
        assertEquals(List.of("a", "d", "e", "b", "c"), CanvasObjectOrder.reorder(source, selected, 2));
        assertEquals(List.of("b", "c", "a", "d", "e"), CanvasObjectOrder.reorder(source, selected, -2));
        assertEquals(source, CanvasObjectOrder.reorder(source, Set.of("e"), 1));
        assertEquals(source, CanvasObjectOrder.reorder(source, Set.of("a"), -1));
    }
}
