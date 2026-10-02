package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResponsiveActionGroupTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void widthPolicyChangesOnlyBelowTheDeclaredBreakpoint() {
        assertFalse(ResponsiveActionGroup.shouldCompact(0, 520));
        assertFalse(ResponsiveActionGroup.shouldCompact(520, 520));
        assertTrue(ResponsiveActionGroup.shouldCompact(519, 520));
    }

    @Test
    void compactPresentationKeepsIconTooltipAndAccessibleName() throws Exception {
        onFx(() -> {
            Button button = ActionButtonFactory.secondary("Siguientes bloques");
            ResponsiveActionGroup.apply(button, "Siguientes bloques", true);
            assertEquals("", button.getText());
            assertEquals(ContentDisplay.GRAPHIC_ONLY, button.getContentDisplay());
            assertNotNull(button.getGraphic());
            assertNotNull(button.getTooltip());
            assertEquals("Siguientes bloques", button.getTooltip().getText());
            assertEquals("Siguientes bloques", button.getAccessibleText());
            assertEquals(40, button.getPrefWidth());

            ResponsiveActionGroup.apply(button, "Siguientes bloques", false);
            assertEquals("Siguientes bloques", button.getText());
            assertEquals(ContentDisplay.LEFT, button.getContentDisplay());
        });
    }

    private static void onFx(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }
}
