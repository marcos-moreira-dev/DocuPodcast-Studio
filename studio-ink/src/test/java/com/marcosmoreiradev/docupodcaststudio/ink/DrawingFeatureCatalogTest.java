package com.marcosmoreiradev.docupodcaststudio.inkcatalog;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingToolId;
import com.marcosmoreiradev.docupodcaststudio.ink.InkEditorController;
import com.marcosmoreiradev.docupodcaststudio.ink.ViewportMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DrawingFeatureCatalogTest {
    @Test
    void theatreFrameIsADeclarativeFixedComposition() {
        DrawingProfile profile = DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.THEATRE_FRAME);
        assertEquals(ViewportMode.FIXED, profile.viewportMode());
        assertEquals(1280, profile.logicalWidth());
        assertEquals(720, profile.logicalHeight());
        assertEquals(50, profile.historyLimit());
        assertTrue(profile.supports(DrawingToolId.PEN));
    }

    @Test
    void sharedHistoryIsBoundedAndRestoresUndoRedo() {
        List<String> state = new ArrayList<>();
        InkEditorController<List<String>> history = new InkEditorController<>(2,
                () -> List.copyOf(state), snapshot -> { state.clear(); state.addAll(snapshot); });
        history.checkpoint(); state.add("a");
        history.checkpoint(); state.add("b");
        assertTrue(history.undo());
        assertEquals(List.of("a"), state);
        assertTrue(history.redo());
        assertEquals(List.of("a", "b"), state);
        assertFalse(history.canRedo());
    }
}
